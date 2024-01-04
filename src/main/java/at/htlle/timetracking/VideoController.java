package at.htlle.timetracking;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;

@RestController
public class VideoController {
    @Autowired
    private VideoRepository videoRepository;
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

            // Save video data to the database
            Video video = new Video();
            video.setName(fileName); // Use the generated file name
            video.setPath("/uploaded-videos/" + fileName);
            video.setSize(file.getSize()); // Save the file size
            video.setUploadDate(LocalDateTime.now()); // Save the upload date
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

    @DeleteMapping("/delete-videos")
    public ResponseEntity<Void> deleteVideos() {
        try {
            // Delete all videos from the database
            videoRepository.deleteAll();

            // Delete all video files from the file system
            Path directory = Paths.get(System.getProperty("user.dir") + "/uploaded-videos/");
            Files.walk(directory)
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



    private String generateFileName(String extension) {
        // Get the current max ID from the database and add 1
        long currentNumber = videoRepository.findMaxId() + 1;
        return "video_" + currentNumber + extension;
    }

    private void saveFile(MultipartFile file, String fileName) throws IOException {
        File directory = new File(System.getProperty("user.dir") + "/uploaded-videos/");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        file.transferTo(new File(directory, fileName));
    }


}