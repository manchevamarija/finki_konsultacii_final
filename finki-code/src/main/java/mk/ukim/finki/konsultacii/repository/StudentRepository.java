package mk.ukim.finki.konsultacii.repository;

import mk.ukim.finki.konsultacii.model.Student;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface StudentRepository extends JpaSpecificationRepository<Student, String> {

    @Query("""
            SELECT s
            FROM Student s
            WHERE LOWER(s.primaryEmail) = LOWER(:email)
               OR LOWER(s.secondaryEmail) = LOWER(:email)
            """)
    Optional<Student> findByEmail(@Param("email") String email);
}
