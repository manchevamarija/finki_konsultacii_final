package mk.ukim.finki.konsultacii.repository;

import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import mk.ukim.finki.konsultacii.model.views.ConsultationView;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


public interface ConsultationViewRepository extends JpaSpecificationRepository<ConsultationView, Long> {

}
