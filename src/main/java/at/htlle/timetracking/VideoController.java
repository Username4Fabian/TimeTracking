package at.htlle.timetracking;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

@RestController
public class VideoController {
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private RaceParticipantRepository raceParticipantRepository;
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("video") MultipartFile file) {
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

            // Save video data to the database
            Video video = new Video();
            video.setName(fileName); // Use the generated file name
            video.setPath("/uploaded-videos/" + fileName); // Save the relative path
            video.setSize(file.getSize()); // Save the file size
            video.setUploadDate(LocalDateTime.now()); // Save the upload date
            video.setThumbnailPath("/uploaded-thumbnails/" + fileName + ".jpg"); // Save the relative path of the thumbnail
            videoRepository.save(video);

            return "File uploaded successfully: " + fileName;
        } catch (Exception e) {
            return "Upload failed: " + e.getMessage();
        }
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
            Path path = Paths.get(System.getProperty("user.dir") + "/src/main/resources/static/uploaded-videos/" + fileName);
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
        mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
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