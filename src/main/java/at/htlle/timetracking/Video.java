package at.htlle.timetracking;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "VIDEO_SEQ")
    @SequenceGenerator(name = "VIDEO_SEQ", sequenceName = "VIDEO_SEQ", allocationSize = 1)
    private Long id;
    private String name;
    private String path;
    private Long size;
    private LocalDateTime uploadDate;
    private String thumbnailPath;
    private LocalDateTime startTime;
    private Long StartNr;

    public Video() {
    }

    public Video(String name, String path, Long size, LocalDateTime uploadDate, String thumbnailPath, LocalDateTime startTime, Long StartNr) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.uploadDate = uploadDate;
        this.thumbnailPath = thumbnailPath;
        this.StartNr = StartNr;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Long getStartNr() {
        return StartNr;
    }

    public void setStartNr(Long startNr) {
        StartNr = startNr;
    }
}