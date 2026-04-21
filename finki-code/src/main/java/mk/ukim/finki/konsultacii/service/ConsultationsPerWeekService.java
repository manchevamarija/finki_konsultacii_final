package mk.ukim.finki.konsultacii.service;

import mk.ukim.finki.konsultacii.model.ConsultationsPerWeek;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface ConsultationsPerWeekService {

    Page<ConsultationsPerWeek> findAll(int page, int size, Integer consultationsNumber, String professorId, String semesterCode,
                                       LocalDate weekStart);
}
