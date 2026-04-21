package mk.ukim.finki.konsultacii.service.implementation;

import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.ConsultationsPerWeek;
import mk.ukim.finki.konsultacii.repository.ConsultationsPerWeekRepository;
import mk.ukim.finki.konsultacii.service.ConsultationsPerWeekService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static mk.ukim.finki.konsultacii.specifications.FieldFilterSpecification.*;

@Service
@AllArgsConstructor
public class ConsultationsPerWeekServiceImpl implements ConsultationsPerWeekService {

    private final ConsultationsPerWeekRepository consultationsPerWeekRepository;


    @Override
    public Page<ConsultationsPerWeek> findAll(int page, int size, Integer consultationsNumber, String professorId, String semesterCode,
                                              LocalDate weekStart) {
        Specification<ConsultationsPerWeek> spec = Specification.where(null);

        if (consultationsNumber != null) {
            spec = spec.and(filterEquals(ConsultationsPerWeek.class,"numberOfConsultations", consultationsNumber));
        }

        if(semesterCode != null && !semesterCode.isEmpty()) {
            spec = spec.and(filterEquals(ConsultationsPerWeek.class, "semester", semesterCode));
        }

        if (professorId != null && !professorId.isEmpty()) {
            spec = spec.and(
                    filterContainsText(ConsultationsPerWeek.class, "professor.id", professorId)
                            .or(filterContainsText(ConsultationsPerWeek.class, "professor.name", professorId))
            );
        }

        if (weekStart != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("weekStart"), weekStart));
        }

        return consultationsPerWeekRepository.findAll(spec, PageRequest.of(page-1, size, Sort.by(
                Sort.Order.desc("weekStart"),
                Sort.Order.asc("professor.id")
        )));
    }
}
