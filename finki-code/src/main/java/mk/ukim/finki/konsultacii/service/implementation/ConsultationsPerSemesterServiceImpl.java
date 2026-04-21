package mk.ukim.finki.konsultacii.service.implementation;

import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.ConsultationsPerSemester;
import mk.ukim.finki.konsultacii.repository.ConsultationsPerSemesterRepository;
import mk.ukim.finki.konsultacii.service.ConsultationsPerSemesterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import static mk.ukim.finki.konsultacii.specifications.FieldFilterSpecification.filterContainsText;
import static mk.ukim.finki.konsultacii.specifications.FieldFilterSpecification.filterEquals;

@Service
@AllArgsConstructor
public class ConsultationsPerSemesterServiceImpl implements ConsultationsPerSemesterService {

    private final ConsultationsPerSemesterRepository consultationsPerSemesterRepository;


    @Override
    public Page<ConsultationsPerSemester> findAll(int page, int size, Integer consultationsNumber, String professorId, String semesterCode) {
        Specification<ConsultationsPerSemester> spec = Specification.where(null);

        if (consultationsNumber != null) {
            spec = spec.and(filterEquals(ConsultationsPerSemester.class,"numberOfConsultations", consultationsNumber));
        }

        if(semesterCode != null && !semesterCode.isEmpty()) {
            spec = spec.and(filterEquals(ConsultationsPerSemester.class, "semester", semesterCode));
        }

        if (professorId != null && !professorId.isEmpty()) {
            spec = spec.and(
                    filterContainsText(ConsultationsPerSemester.class, "professor.id", professorId)
                            .or(filterContainsText(ConsultationsPerSemester.class, "professor.name", professorId))
            );
        }

        return consultationsPerSemesterRepository.findAll(spec, PageRequest.of(page-1, size, Sort.by(
                Sort.Order.desc("semesterStart"),
                Sort.Order.asc("professor.id")
        )));
    }
}
