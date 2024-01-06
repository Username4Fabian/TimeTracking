package at.htlle.timetracking;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RaceParticipantRepository extends JpaRepository<RaceParticipant, Long> {
    @Query(value = "ALTER SEQUENCE RACEPARTICIPANT_SEQ RESTART WITH 1", nativeQuery = true)
    @Modifying
    @Transactional
    void resetSequence();
}