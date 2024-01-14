package at.htlle.timetracking;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.mediatool.event.VideoPictureEvent;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;

@RestController
public class VideoController {
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private RaceParticipantRepository raceParticipantRepository;
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("video") MultipartFile file, @RequestParam("startTime") String startTime) {
        try {
            String originalFileName = file.getOriginalFilename();
            assert originalFileName != null;
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

            String fileName = generateFileName(extension);

            double fileSizeInMb = file.getSize() / (1024.0 * 1024.0);
            logger.info("Received file: " + originalFileName + ", size: " + fileSizeInMb + " MB");
            logger.info("File size: " + file.getSize());
            saveFile(file, fileName);

            // Generate thumbnail
            String videoPath = System.getProperty("user.dir") + "/src/main/resources/static/uploaded-videos/" + fileName;
            String thumbnailPath = System.getProperty("user.dir") + "/src/main/resources/static/uploaded-thumbnails/" + fileName + ".jpg";
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                generateThumbnail(videoPath, thumbnailPath);
            });
            future.get(); // Wait for the thumbnail generation to complete

            // Parse the startTime string
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            LocalDateTime parsedStartTime = LocalDateTime.parse(startTime.substring(0, 23), formatter);

            // Save video data to the database
            Video video = new Video();
            video.setName(fileName); // Use the generated file name
            video.setPath("/uploaded-videos/" + fileName); // Save the relative path
            video.setSize(file.getSize()); // Save the file size
            video.setUploadDate(LocalDateTime.now()); // Save the upload date
            video.setThumbnailPath("/uploaded-thumbnails/" + fileName + ".jpg"); // Save the relative path of the thumbnail
            video.setStartTime(parsedStartTime); // Save the start time
            videoRepository.save(video);

            // Process the video to add timestamps
            String processedVideoPath = processVideo(video.getPath(), parsedStartTime);

            // Delete the original video file
            Files.delete(Paths.get(System.getProperty("user.dir") + "/src/main/resources/static" + video.getPath()));

            // Update the video record to point to the new video file
            video.setPath(processedVideoPath);
            videoRepository.save(video);

            return "File uploaded successfully: " + fileName;
        } catch (Exception e) {
            return "Upload failed: " + e.getMessage();
        }
    }

    private String processVideo(String videoPath, LocalDateTime startTime) {
        String inputPath = System.getProperty("user.dir") + "/src/main/resources/static" + videoPath;
        String outputPath = inputPath.replace(".mp4", "_processed.mp4");

        // Adjust the start time to the start of the day
        LocalDateTime startOfDay = startTime.toLocalDate().atStartOfDay();
        Duration durationSinceStartOfDay = Duration.between(startOfDay, startTime);

        // Convert the duration to microseconds
        long startTimeMicros = durationSinceStartOfDay.toMillis() * 1000;

        IMediaReader mediaReader = ToolFactory.makeReader(inputPath);
        IMediaWriter mediaWriter = ToolFactory.makeWriter(outputPath, mediaReader);

        mediaReader.addListener(new MediaListenerAdapter() {
            @Override
            public void onVideoPicture(IVideoPictureEvent event) {
                // Get the video picture
                IVideoPicture pic = event.getPicture();

                // Convert the picture to an image
                BufferedImage image = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                IConverter converter = ConverterFactory.createConverter(image, pic.getPixelType());
                image = converter.toImage(pic);

                // Create a graphics object from the image
                Graphics graphics = image.getGraphics();

                // Add the timestamp to the image
                graphics.setColor(Color.WHITE);
                graphics.setFont(new Font("Arial", Font.BOLD, 30));

                // Add the start time to the frame's timestamp
                long timestampInMicros = pic.getTimeStamp() + startTimeMicros;

                // Convert the timestamp to milliseconds
                long timestampInMillis = timestampInMicros / 1000;

                // Convert the timestamp to hours, minutes, seconds, and milliseconds
                Duration duration = Duration.ofMillis(timestampInMillis);
                long hours = duration.toHours();
                duration = duration.minusHours(hours);
                long minutes = duration.toMinutes();
                duration = duration.minusMinutes(minutes);
                long seconds = duration.getSeconds();
                long millis = duration.minusSeconds(seconds).toMillis();

                // Format the timestamp as a clock time
                String timestamp = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
                graphics.drawString("Timestamp: " + timestamp, 10, 30);

                // Dispose the graphics object
                graphics.dispose();

                // Create a new event with the modified image
                IVideoPictureEvent modifiedEvent = new VideoPictureEvent(event.getSource(), image, pic.getTimeStamp(), event.getTimeUnit(), event.getStreamIndex());
                mediaWriter.onVideoPicture(modifiedEvent);
                mediaWriter.onVideoPicture(event);
            }
        });

        mediaReader.addListener(mediaWriter);

        while (mediaReader.readPacket() == null) ;

        System.out.println("Video processed successfully: " + outputPath);
        return videoPath.replace(".mp4", "_processed.mp4");
    }

    @GetMapping("/videos")
    public Iterable<Video> getVideos() {
        return videoRepository.findAll();
    }

    @DeleteMapping("/delete-racers")
    public ResponseEntity<Void> deleteRacers() {
        try {
            // Delete all racers from the database
            raceParticipantRepository.deleteAll();
            raceParticipantRepository.resetSequence();


            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting racers: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/delete-videos")
    public ResponseEntity<Void> deleteVideos() {
        try {
            // Delete all videos from the database
            videoRepository.deleteAll();

            // Delete all video files from the file system
            Path videosDirectory = Paths.get(System.getProperty("user.dir") + "/src/main/resources/static/uploaded-videos/");
            Path thumbnailsDirectory = Paths.get(System.getProperty("user.dir") + "/src/main/resources/static/uploaded-thumbnails/");
            Files.walk(videosDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.walk(thumbnailsDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            // Reset the sequence
            videoRepository.resetSequence();

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting videos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/uploaded-thumbnails/{fileName}")
    public ResponseEntity<Resource> serveThumbnail(@PathVariable String fileName) {
        try {
            Path path = Paths.get(System.getProperty("user.dir") + "/src/main/resources/static/uploaded-thumbnails/" + fileName);
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"").body(resource);
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @GetMapping("/uploaded-videos/{fileName}")
    public ResponseEntity<Resource> serveVideoFile(@PathVariable String fileName) {
        try {
            String processedFileName = fileName.replace(".mp4", "_processed.mp4");
            Path path = Paths.get(System.getProperty("user.dir") + "/src/main/resources/static/uploaded-videos/" + processedFileName);
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"").body(resource);
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    private String generateFileName(String extension) {
        // Get the current max ID from the database and add 1
        long currentNumber = videoRepository.findMaxId() + 1;
        return "video_" + currentNumber + extension;
    }

    private void saveFile(MultipartFile file, String fileName) throws IOException {
        File directory = new File(System.getProperty("user.dir") + "/src/main/resources/static/uploaded-videos/");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        file.transferTo(new File(directory, fileName));
    }

    private void generateThumbnail(String videoPath, String thumbnailPath) {
        // Create the directory if it doesn't exist
        File directory = new File(thumbnailPath).getParentFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }

        IMediaReader mediaReader = ToolFactory.makeReader(videoPath);
        mediaReader.setBufferedImageTypeToGenerate(TYPE_3BYTE_BGR);
        mediaReader.addListener(new MediaListenerAdapter() {
            @Override
            public void onVideoPicture(IVideoPictureEvent event) {
                if (event.getTimeStamp() >= 2 * 1000000) { // get frame at or after 2 seconds
                    File outputfile = new File(thumbnailPath);
                    try {
                        ImageIO.write(event.getImage(), "jpg", outputfile);
                        logger.info("Thumbnail generated at: " + thumbnailPath);
                    } catch (IOException e) {
                        logger.error("Error generating thumbnail: ", e);
                    }
                    mediaReader.removeListener(this); // remove listener to stop processing
                }
            }
        });
        while (mediaReader.readPacket() == null) ; // start processing
    }



}