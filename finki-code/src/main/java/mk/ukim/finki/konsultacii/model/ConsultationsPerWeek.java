package mk.ukim.finki.konsultacii.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDate;

@Entity
@Table(name = "consultations_per_week_view")
@Immutable
@Getter
public class ConsultationsPerWeek {
    @Id
    private String id;

    @Column(name = "week_start")
    private LocalDate weekStart;

    @Column(name = "week_end")
    private LocalDate weekEnd;

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