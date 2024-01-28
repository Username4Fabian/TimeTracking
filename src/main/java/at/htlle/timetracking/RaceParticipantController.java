package at.htlle.timetracking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping
    public List<RaceParticipant> getRaceParticipants() {
        List<RaceParticipant> participants = raceParticipantRepository.findAll();
        return participants.stream()
                .sorted(Comparator.comparing(RaceParticipant::getFinishTime))
                .collect(Collectors.toList());
    }
}