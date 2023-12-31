package at.htlle.timetracking;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class RaceParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RACEPARTICIPANT_SEQ")
    @SequenceGenerator(name = "RACEPARTICIPANT_SEQ", sequenceName = "RACEPARTICIPANT_SEQ", allocationSize = 1)
    private Long id;

    private int startNr;
    private LocalDateTime finishTime;
    private String name;
    private LocalDateTime databaseEntryTime;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getStartNr() {
        return startNr;
    }

    public void setStartNr(int startNr) {
        this.startNr = startNr;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getDatabaseEntryTime() {
        return databaseEntryTime;
    }

    public void setDatabaseEntryTime(LocalDateTime databaseEntryTime) {
        this.databaseEntryTime = databaseEntryTime;
    }

    @PrePersist
    public void prePersist() {
        this.databaseEntryTime = LocalDateTime.now();
    }
}
