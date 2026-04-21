package mk.ukim.finki.konsultacii.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Student {

    @Id
    @Column(name = "student_index")
    private String index;

    @Column(name = "email")
    private String primaryEmail;

    @Column(name = "secondary_email")
    private String secondaryEmail;

    private String name;

    private String lastName;

    private String parentName;

    @ManyToOne
    private StudyProgram studyProgram;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Student student = (Student) o;
        return getIndex() != null && Objects.equals(getIndex(), student.getIndex());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getEmail() {
        return primaryEmail;
    }

    public void setEmail(String email) {
        this.primaryEmail = email;
    }

    public boolean hasEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return getCalendarEmails().stream().anyMatch(email::equalsIgnoreCase);
    }

    public List<String> getCalendarEmails() {
        Set<String> emails = new LinkedHashSet<>();
        addIfPresent(emails, primaryEmail);
        addIfPresent(emails, secondaryEmail);
        return List.copyOf(emails);
    }

    private void addIfPresent(Set<String> emails, String email) {
        if (email != null && !email.isBlank()) {
            emails.add(email.trim());
        }
    }

    public String getFullName() {
        return name + " " + lastName;
    }
}

