package mk.ukim.finki.konsultacii.model.dtos;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;


@Getter
public class IrregularConsultationsFormDto extends ConsultationFormDto {
    @NotNull(message = "Датумот за нерегуларни консултации не смее да биде null")
    @FutureOrPresent(message = "Датумот за дополнителни консултации не смее да биде во минатото")
    LocalDate date;

    public IrregularConsultationsFormDto(
            LocalTime startTime, LocalTime endTime, String roomName, Boolean online,
                String studentInstructions, String meetingLink, LocalDate date) {
        super(startTime, endTime, online, studentInstructions, roomName, meetingLink);
        this.date = date;
    }
}
