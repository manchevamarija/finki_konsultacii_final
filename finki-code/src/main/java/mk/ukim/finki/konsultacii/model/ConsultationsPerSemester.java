package mk.ukim.finki.konsultacii.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDate;

@Entity
@Table(name = "consultations_per_semester_view")
@Immutable
@Getter
public class ConsultationsPerSemester {
    @Id
    private String id;

    @Column(name = "semester_start")
    private LocalDate semesterStart;

    @Column(name = "semester_end")
    private LocalDate semesterEnd;

    @ManyToOne
    @JoinColumn(name = "professor_id", referencedColumnName = "id", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Professor professor;

    @Column(name = "number_of_consultations")
    private Integer numberOfConsultations;

    @Column(name = "number_of_attendances")
    private Integer numberOfAttendances;

    String semester;

}