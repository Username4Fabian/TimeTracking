package at.htlle.timetracking.models;

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
    @SuppressWarnings("unused")
    private LocalDateTime databaseEntryTime;

    public RaceParticipant() {
    }

    @PrePersist
    public void prePersist() {
        this.databaseEntryTime = LocalDateTime.now();
    }

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

}
