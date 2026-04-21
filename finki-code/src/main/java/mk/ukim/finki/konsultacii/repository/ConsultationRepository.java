package mk.ukim.finki.konsultacii.repository;

import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaSpecificationRepository<Consultation, Long> {

    @Query("SELECT c FROM Consultation c WHERE c.professor.id = :professorId AND c.type = :type " +
            "AND c.status = 'ACTIVE' AND c.oneTimeDate BETWEEN :startDate AND :endDate " +
            "ORDER BY c.oneTimeDate, c.startTime")
    List<Consultation> findConsultationsForNextWeek(String professorId,
                                                    ConsultationType type,
                                                    LocalDate startDate,
                                                    LocalDate endDate);

    @Query("SELECT c FROM Consultation c WHERE c.professor.id = :professorId AND c.status = 'ACTIVE' " +
            "AND ((c.type = 'WEEKLY' AND c.oneTimeDate BETWEEN :today AND :nextWeek) " +
            "OR (c.type = 'ONE_TIME' AND c.oneTimeDate BETWEEN :today AND :nextTwoWeeks))")
    List<Consultation> findNextWeekActiveConsultations(@Param("professorId") String professorId,
                                                       @Param("today") LocalDate today,
                                                       @Param("nextWeek") LocalDate nextWeek,
                                                       @Param("nextTwoWeeks") LocalDate nextTwoWeeks);

    Optional<Consultation> findByProfessor_IdAndGoogleCalendarEventId(String professorId, String googleCalendarEventId);

    List<Consultation> findAllByProfessor_IdAndGoogleCalendarEventIdIsNotNullAndStatus(
            String professorId,
            ConsultationStatus status
    );

    Optional<Consultation> findByProfessor_IdAndOutlookEventId(String professorId, String outlookEventId);

    List<Consultation> findAllByProfessor_IdAndOutlookEventIdIsNotNullAndStatus(
            String professorId,
            ConsultationStatus status
    );

    List<Consultation> findAllByProfessor_IdAndTypeAndWeeklyDayOfWeekAndStartTimeAndEndTimeAndOneTimeDateAfter(
            String professorId,
            ConsultationType type,
            DayOfWeek weeklyDayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate oneTimeDate
    );
}
