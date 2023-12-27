package at.htlle.timetracking;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class VideoController {

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

            return "File uploaded successfully: " + fileName;
        } catch (Exception e) {
            return "Upload failed: " + e.getMessage();
        }
    }



    private String generateFileName(String extension) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now) + extension;
    }

    private void saveFile(MultipartFile file, String fileName) throws IOException {
        File directory = new File(System.getProperty("user.dir") + "/uploaded-videos/");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        file.transferTo(new File(directory, fileName));
    }
}