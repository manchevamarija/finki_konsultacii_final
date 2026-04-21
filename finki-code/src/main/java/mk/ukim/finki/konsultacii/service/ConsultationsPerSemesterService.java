package mk.ukim.finki.konsultacii.service;

import mk.ukim.finki.konsultacii.model.ConsultationsPerSemester;
import org.springframework.data.domain.Page;

public interface ConsultationsPerSemesterService {

    Page<ConsultationsPerSemester> findAll(int page, int size, Integer consultationsNumber, String professorId, String semesterCode);
}
