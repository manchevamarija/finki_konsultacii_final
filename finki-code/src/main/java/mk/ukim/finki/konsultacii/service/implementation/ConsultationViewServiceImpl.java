package mk.ukim.finki.konsultacii.service.implementation;

import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationTimeFilter;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import mk.ukim.finki.konsultacii.model.views.ConsultationView;
import mk.ukim.finki.konsultacii.repository.ConsultationViewRepository;
import mk.ukim.finki.konsultacii.service.ConsultationViewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static mk.ukim.finki.konsultacii.specifications.FieldFilterSpecification.*;

@Service
@AllArgsConstructor
public class ConsultationViewServiceImpl implements ConsultationViewService {

    public ConsultationViewRepository consultationViewRepository;

    @Override
    public Page<ConsultationView> findConsultations(String professorId, ConsultationType type, ConsultationTimeFilter time, ConsultationStatus status,
                                                    int pageSize, int pageNum) {
        Specification<ConsultationView> spec = Specification
                .where(filterEquals(ConsultationView.class, "professor.id", professorId));

        if (type != null) {
            spec = spec.and(filterEqualsV(ConsultationView.class, "type", type));
        }
        if (status != null) {
            spec = spec.and(filterEqualsV(ConsultationView.class, "status", status));
        }

        LocalDateTime now = LocalDateTime.now();
        Sort sort;
        if (time == ConsultationTimeFilter.UPCOMING) {
            spec = spec.and(isUpcoming(ConsultationView.class, "oneTimeDate", "endTime", now));
            sort = Sort.by(Sort.Order.asc("oneTimeDate"), Sort.Order.asc("startTime"));
        } else if (time == ConsultationTimeFilter.PAST) {
            spec = spec.and(isPast(ConsultationView.class, "oneTimeDate", "endTime", now));
            sort = Sort.by(Sort.Order.desc("oneTimeDate"), Sort.Order.desc("startTime"));
        } else if (time == ConsultationTimeFilter.UPCOMING_TODAY) {
            spec = spec.and(greaterThanOrEqualTo(ConsultationView.class, "oneTimeDate", now.toLocalDate()));
            sort = Sort.by(Sort.Order.asc("oneTimeDate"), Sort.Order.asc("startTime"));
        } else {
            sort = Sort.by(Sort.Order.asc("oneTimeDate"), Sort.Order.asc("startTime"));
        }

        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        return consultationViewRepository.findAll(spec, pageable);
    }
}
