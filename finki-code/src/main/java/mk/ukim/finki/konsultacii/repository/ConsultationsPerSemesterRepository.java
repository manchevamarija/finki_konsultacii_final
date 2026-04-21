package mk.ukim.finki.konsultacii.repository;

import mk.ukim.finki.konsultacii.model.ConsultationsPerSemester;
import mk.ukim.finki.konsultacii.model.ConsultationsPerWeek;

import java.util.List;

public interface ConsultationsPerSemesterRepository extends JpaSpecificationRepository<ConsultationsPerSemester, String> {
    List<ConsultationsPerSemester> findByProfessorId(String professorId);
}
