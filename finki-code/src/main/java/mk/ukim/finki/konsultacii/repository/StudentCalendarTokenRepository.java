package mk.ukim.finki.konsultacii.repository;

import mk.ukim.finki.konsultacii.calendar.model.StudentCalendarToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentCalendarTokenRepository extends JpaRepository<StudentCalendarToken, String> {
    Optional<StudentCalendarToken> findByStudentIndex(String studentIndex);
}
