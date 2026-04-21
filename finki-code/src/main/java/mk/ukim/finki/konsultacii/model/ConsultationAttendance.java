package mk.ukim.finki.konsultacii.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "consultation_attendance")
@Getter
@Setter
public class ConsultationAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Student attendee;
    @Getter
    @ManyToOne
    private Consultation consultation;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<Comment> comments = new ArrayList<>();

    private Boolean reportAbsentProfessor;

    private Boolean reportAbsentStudent;

    private String absentProfessorComment;

    private String absentStudentComment;

    @Column(name = "student_google_calendar_event_id")
    private String studentGoogleCalendarEventId;

    public ConsultationAttendance(Student attendee, Consultation consultation) {
        this.attendee = attendee;
        this.consultation = consultation;
        this.comments = new ArrayList<>();
        this.reportAbsentProfessor = false;
        this.reportAbsentStudent = false;
    }

    public ConsultationAttendance() {

    }


    public boolean isStarted() {
        return this.consultation.isStarted();
    }

    public boolean isActive() {
        return this.consultation.isActive();
    }

    public boolean isEnded() {
        return this.consultation.isEnded();
    }

    public LocalTime getConsultationStartTime(){
        return this.consultation.getStartTime();
    }

    public LocalDate getConsultationOneTimeDate(){
        return this.consultation.getOneTimeDate();
    }
}
