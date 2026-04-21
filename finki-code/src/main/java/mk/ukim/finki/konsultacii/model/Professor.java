package mk.ukim.finki.konsultacii.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mk.ukim.finki.konsultacii.model.enumerations.ProfessorTitle;
import org.hibernate.Hibernate;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.Objects;


@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Where(clause = "active = true")
public class Professor {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private ProfessorTitle title;

    private boolean active;

    public Professor(String name, String email, ProfessorTitle title) {
        this.name = name;
        this.email = email;
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Professor professor = (Professor) o;
        return getId() != null && Objects.equals(getId(), professor.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

