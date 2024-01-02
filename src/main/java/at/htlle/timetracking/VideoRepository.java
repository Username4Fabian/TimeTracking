package at.htlle.timetracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    @Query("SELECT COALESCE(MAX(v.id), 0) FROM Video v")
    Long findMaxId();
}