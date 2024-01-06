package at.htlle.timetracking;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RaceParticipantRepository extends JpaRepository<RaceParticipant, Long> {
}