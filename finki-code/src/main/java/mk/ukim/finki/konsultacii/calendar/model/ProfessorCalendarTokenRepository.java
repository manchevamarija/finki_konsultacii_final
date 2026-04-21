package mk.ukim.finki.konsultacii.calendar.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfessorCalendarTokenRepository extends JpaRepository<ProfessorCalendarToken, String> {
    Optional<ProfessorCalendarToken> findByProfessorId(String professorId);
}
