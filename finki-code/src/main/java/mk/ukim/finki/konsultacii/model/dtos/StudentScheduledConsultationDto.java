package mk.ukim.finki.konsultacii.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mk.ukim.finki.konsultacii.model.Room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Getter
@AllArgsConstructor
public class StudentScheduledConsultationDto {

    private Long id;

    private String professorName;

    private LocalDate oneTimeDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Room room;

    private String comment;

    public LocalDateTime getOneTimeStart() {
        return this.oneTimeDate.atTime(this.startTime);
    }


    public boolean isStarted() {
        return LocalDateTime.now().isAfter(this.getOneTimeStart());
    }

    public boolean isActive() {
        return LocalDateTime.now().isAfter(this.getOneTimeStart()) && !isEnded();
    }

    public boolean isEnded() {
        return LocalDateTime.now().isAfter(this.oneTimeDate.atTime(this.endTime));
    }
}
