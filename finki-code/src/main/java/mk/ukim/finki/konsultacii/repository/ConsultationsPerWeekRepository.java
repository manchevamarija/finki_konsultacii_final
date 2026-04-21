package mk.ukim.finki.konsultacii.repository;

import mk.ukim.finki.konsultacii.model.ConsultationsPerWeek;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ConsultationsPerWeekRepository extends JpaSpecificationRepository<ConsultationsPerWeek, String> {
    List<ConsultationsPerWeek> findByProfessorId(String professorId);
}
