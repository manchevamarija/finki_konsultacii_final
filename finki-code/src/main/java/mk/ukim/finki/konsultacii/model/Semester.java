package mk.ukim.finki.konsultacii.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Semester {

    // 2022/2023-W
    @Id
    private String code;

    // 2022/2023
    private String year;

    @Enumerated(EnumType.STRING)
    private SemesterType semesterType;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate enrollmentStartDate;

    private LocalDate enrollmentEndDate;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<StudyCycle> cycle;

    @Enumerated(EnumType.STRING)
    private SemesterState state;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Semester semester = (Semester) o;
        return getCode() != null && Objects.equals(getCode(), semester.getCode());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public LocalDate getEffectiveStartDate() {
        if (semesterType == SemesterType.SUMMER) {
            return LocalDate.of(getEndYear(), 2, 15);
        } else { // WINTER
            return LocalDate.of(getStartYear(), 9, 15);
        }
    }

    public LocalDate getEffectiveEndDate() {
        int endYear = getEndYear();

        if (semesterType == SemesterType.SUMMER) {
            return LocalDate.of(endYear, 9, 14);
        } else { // WINTER
            return LocalDate.of(endYear, 2, 14);
        }
    }

    private int getStartYear() {
        String[] yearParts = year.split("-");
        return Integer.parseInt("20" + yearParts[0].substring(2)); // "2023-24" -> 2023
    }

    private int getEndYear() {
        String[] yearParts = year.split("-");
        return Integer.parseInt("20" + yearParts[1]); // "2023-24" -> 2024
    }

    public boolean isCurrent() {
        LocalDate today = LocalDate.now();
        return (today.isEqual(getEffectiveStartDate()) || today.isAfter(getEffectiveStartDate()))
                && (today.isEqual(getEffectiveEndDate()) || today.isBefore(getEffectiveEndDate()));
    }

}
