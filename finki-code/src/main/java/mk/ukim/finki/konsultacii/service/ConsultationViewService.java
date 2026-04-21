package mk.ukim.finki.konsultacii.service;

import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationTimeFilter;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import mk.ukim.finki.konsultacii.model.views.ConsultationView;
import org.springframework.data.domain.Page;

public interface ConsultationViewService {
    Page<ConsultationView> findConsultations(String professorId, ConsultationType type, ConsultationTimeFilter time, ConsultationStatus status,
                                             int pageSize, int pageNum);
}
