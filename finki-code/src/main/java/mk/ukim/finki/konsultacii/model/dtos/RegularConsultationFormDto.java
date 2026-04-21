package mk.ukim.finki.konsultacii.model.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalTime;


@Getter
@Setter
public class RegularConsultationFormDto extends ConsultationFormDto {
    @NotNull(message = "Денот во неделата за регуларни консултации мора да биде специфициран")
    DayOfWeek dayOfWeek;

    String semesterCode;

    public RegularConsultationFormDto(
            LocalTime startTime, LocalTime endTime, String roomName, Boolean online,
            String studentInstructions, String meetingLink, DayOfWeek dayOfWeek) {
        super(startTime, endTime, online, studentInstructions, roomName, meetingLink);
        this.dayOfWeek = dayOfWeek;
    }

    public RegularConsultationFormDto(
            LocalTime startTime, LocalTime endTime, String roomName, Boolean online,
            String studentInstructions, String meetingLink, DayOfWeek dayOfWeek, String semesterCode) {
        super(startTime, endTime, online, studentInstructions, roomName, meetingLink);
        this.dayOfWeek = dayOfWeek;
        this.semesterCode = semesterCode;
    }
}
