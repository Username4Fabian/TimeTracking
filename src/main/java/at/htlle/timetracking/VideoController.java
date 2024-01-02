package at.htlle.timetracking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

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