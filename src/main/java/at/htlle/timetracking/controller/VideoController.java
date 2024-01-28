package at.htlle.timetracking.controller;

import at.htlle.timetracking.repositories.VideoRepository;
import at.htlle.timetracking.models.Video;
import at.htlle.timetracking.services.NumberRecognition;
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
import org.springframework.beans.factory.annotation.Value;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;

@RestController
public class VideoController{
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Value("${number.recognition.api.key}")
    private String apiKey;
    @Value("${number.recognition.custom.prompt}")
    private String customPrompt;

    @Autowired
    private VideoRepository videoRepository;

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("video") MultipartFile file, @RequestParam("startTime") String startTime) {
        try{
            String originalFileName = file.getOriginalFilename();
            assert originalFileName != null;
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = generateFileName(extension);
            fileSize(originalFileName, file);
            saveFile(file, fileName);

            String recognizedNumber = thumbnailManager(fileName);
            LocalDateTime parsedStartTime = dateFormatter(startTime);
            saveVideo(fileName, file, recognizedNumber, parsedStartTime);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return startTime;
    }

    @GetMapping("/videos")
    public Iterable<Video> getVideos() {
        return videoRepository.findAll();
    }

    @DeleteMapping("/delete-videos")
    public ResponseEntity<Void> deleteVideos() {
        try {
            videoRepository.deleteAll();
            deleteAllFiles();
            videoRepository.resetSequence();
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error deleting videos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/uploaded-thumbnails/{fileName}")
    public ResponseEntity<Resource> serveThumbnail(@PathVariable String fileName) {
        return serveFileFromDirectory("/src/main/resources/static/uploaded-thumbnails/", fileName);
    }

    @GetMapping("/uploaded-videos/{fileName}")
    public ResponseEntity<Resource> serveVideoFile(@PathVariable String fileName) {
        String processedFileName = fileName.replace(".mp4", "_processed.mp4");
        return serveFileFromDirectory("/src/main/resources/static/uploaded-videos/", processedFileName);
    }


    private ResponseEntity<Resource> serveFileFromDirectory(String directoryPath, String fileName) {
        try {
            Path path = Paths.get(System.getProperty("user.dir") + directoryPath + fileName);
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

    private void deleteAllFiles () throws IOException {
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
    }

    private String processVideo(String videoPath, LocalDateTime startTime) {
        String inputPath = System.getProperty("user.dir") + "/src/main/resources/static" + videoPath;
        String outputPath = inputPath.replace(".mp4", "_processed.mp4");

        long startTimeMicros = Duration.between(startTime.toLocalDate().atStartOfDay(), startTime).toMillis() * 1000;

        IMediaReader mediaReader = ToolFactory.makeReader(inputPath);
        IMediaWriter mediaWriter = ToolFactory.makeWriter(outputPath, mediaReader);

        mediaReader.addListener(new MediaListenerAdapter() {
            @Override
            public void onVideoPicture(IVideoPictureEvent event) {
                BufferedImage image = convertToBufferedImage(event.getPicture());
                Graphics graphics = image.getGraphics();

                drawTimestamp(graphics, event.getPicture().getTimeStamp() + startTimeMicros);

                graphics.dispose();

                IVideoPictureEvent modifiedEvent = new VideoPictureEvent(event.getSource(), image, event.getPicture().getTimeStamp(), event.getTimeUnit(), event.getStreamIndex());
                mediaWriter.onVideoPicture(modifiedEvent);
            }
        });

        mediaReader.addListener(mediaWriter);

        while (mediaReader.readPacket() == null) ;

        System.out.println("Video processed successfully: " + outputPath);
        return videoPath.replace(".mp4", "_processed.mp4");
    }

    private void saveVideo(String fileName, MultipartFile file, String recognizedNumber, LocalDateTime parsedStartTime) throws IOException {
        Video video = new Video();
        video.setName(fileName);
        video.setPath("/uploaded-videos/" + fileName);
        video.setSize(file.getSize());
        video.setUploadDate(LocalDateTime.now());
        video.setThumbnailPath("/uploaded-thumbnails/" + fileName + ".jpg");
        video.setStartTime(parsedStartTime);
        video.setStartNr(Long.valueOf(recognizedNumber));
        videoRepository.save(video);

        String processedVideoPath = processVideo(video.getPath(), parsedStartTime);
        Files.delete(Paths.get(System.getProperty("user.dir") + "/src/main/resources/static" + video.getPath()));
        video.setPath(processedVideoPath);
        videoRepository.save(video);
    }

    private LocalDateTime dateFormatter(String startTime){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(startTime.substring(0, 24), formatter);
        ZonedDateTime zonedDateTime = offsetDateTime.atZoneSameInstant(ZoneId.of("Europe/Vienna"));

        return zonedDateTime.toLocalDateTime();
    }

    private String numberRecognition(String thumbnailPath) throws IOException {
        File thumbnailFile = new File(thumbnailPath);
        NumberRecognition numberRecognition = new NumberRecognition(apiKey, thumbnailFile.getPath(), customPrompt);
        String recognizedNumber = numberRecognition.getNumberFromImage();
        System.out.println("Recognized number: " + recognizedNumber);
        return recognizedNumber;
    }

    private String thumbnailManager(String fileName){
        String videoPath = System.getProperty("user.dir") + "/src/main/resources/static/uploaded-videos/" + fileName;
        String thumbnailPath = System.getProperty("user.dir") + "/src/main/resources/static/uploaded-thumbnails/" + fileName + ".jpg";
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            generateThumbnail(videoPath, thumbnailPath);
        });
        try {
            future.get();// Wait for the thumbnail generation to complete
            return numberRecognition(thumbnailPath);
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        }
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

    private void fileSize(String originalFileName, MultipartFile file){
        double fileSizeInMb = file.getSize() / (1024.0 * 1024.0);
        logger.info("Received file: " + originalFileName + ", size: " + fileSizeInMb + " MB");
        logger.info("File size: " + file.getSize());
    }

    private String generateFileName(String extension){
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

    private BufferedImage convertToBufferedImage(IVideoPicture pic) {
        BufferedImage image = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        IConverter converter = ConverterFactory.createConverter(image, pic.getPixelType());
        return converter.toImage(pic);
    }

    private void drawTimestamp(Graphics graphics, long timestampInMicros) {
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, 30));

        String timestamp = formatTimestamp(timestampInMicros / 1000);
        graphics.drawString(timestamp, 10, 30);
    }

    private String formatTimestamp(long timestampInMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(timestampInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timestampInMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timestampInMillis) % 60;
        long millis = timestampInMillis % 1000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

}
