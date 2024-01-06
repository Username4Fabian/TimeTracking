package at.htlle.timetracking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/race-participants")
public class RaceParticipantController {

    @Autowired
    private RaceParticipantRepository raceParticipantRepository;

    @PostMapping
    public ResponseEntity<RaceParticipant> createRaceParticipant(@RequestBody RaceParticipant raceParticipant) {
        raceParticipantRepository.save(raceParticipant);
        return new ResponseEntity<>(raceParticipant, HttpStatus.CREATED);
    }
}