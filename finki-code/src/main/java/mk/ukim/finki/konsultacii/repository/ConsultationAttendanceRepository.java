package mk.ukim.finki.konsultacii.repository;

import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.ConsultationAttendance;
import mk.ukim.finki.konsultacii.model.Student;
import mk.ukim.finki.konsultacii.model.custom.ConsultationTermDto;
import mk.ukim.finki.konsultacii.model.projections.UserAttendanceProjection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ConsultationAttendanceRepository
        extends JpaSpecificationRepository<ConsultationAttendance, Long> {

    @Query(value = """
                    SELECT ct.id AS id, p.name AS professorName, ct.oneTimeDate AS oneTimeDate, ct.startTime AS timeFrom, ct.endTime AS timeTo, ct.room AS room, a.comments AS comment
                    FROM ConsultationAttendance a
                    JOIN Consultation ct ON a.consultation = ct
                    JOIN Professor p ON ct.professor = p
                    WHERE a.attendee.index = ?1
                    AND ct.oneTimeDate >= CURRENT_DATE
                    AND (ct.status IS NOT NULL AND ct.status = 'ACTIVE' OR ct.type = 'ONE_TIME')
            """)
    List<ConsultationTermDto> findAllFutureActiveConsultationsForStudent(String index);

    @Query(value = """
                SELECT s.email as email, s.name as firstName, s.last_name as lastName, a.comments as comment
                FROM consultation_attendance a
                JOIN student s ON a.attendee_student_index = s.student_index
                WHERE a.consultation_id = ?1
            """, nativeQuery = true
    )
    List<UserAttendanceProjection> findAttendeesForConsultation(Long consultationId);

    @Query("SELECT ca FROM ConsultationAttendance ca WHERE ca.consultation.id = :consultationId")
    List<ConsultationAttendance> findByConsultationId(Long consultationId);

    Optional<ConsultationAttendance> findFirstByConsultation_IdAndAttendee_IndexOrderByIdAsc(
            Long consultationId,
            String studentIndex
    );

    List<ConsultationAttendance> findByAttendee_Index(String studentIndex);

    @Query("SELECT COUNT(ca) FROM ConsultationAttendance ca WHERE ca.consultation.id = :consultationId")
    Long attendancesCountByConsultationId(Long consultationId);

    @Query("SELECT ca FROM ConsultationAttendance ca " +
            "WHERE ca.consultation.id IN :consultationIds AND ca.attendee.index = :studentIndex")
    List<ConsultationAttendance> findByConsultationIdsAndStudentEmail(
            @Param("consultationIds") List<Long> consultationIds,
            @Param("studentIndex") String studentIndex);


    @Modifying
    @Query("DELETE FROM ConsultationAttendance ca WHERE ca.consultation.id = :consultationId")
    void deleteAllByConsultationId(@Param("consultationId") Long consultationId);
}
