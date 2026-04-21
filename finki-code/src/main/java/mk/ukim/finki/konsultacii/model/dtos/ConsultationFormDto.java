package mk.ukim.finki.konsultacii.model.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import mk.ukim.finki.konsultacii.model.validation.RoomIdXORLinkIsNotNull;
import mk.ukim.finki.konsultacii.model.validation.TimeFromIsBeforeTimeTo;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalTime;


@Getter
@AllArgsConstructor
@TimeFromIsBeforeTimeTo
@RoomIdXORLinkIsNotNull
public class ConsultationFormDto {
    @NotNull(message = "Времето на започнување на консултациите мора да биде специфицирано")
    LocalTime startTime;
    @NotNull(message = "Времето на завршување на консултациите мора да биде специфицирано")
    LocalTime endTime;

    Boolean online;

    String studentInstructions;

    // If null, the user has chosen online consultation room
    String roomName;

    String meetingLink;

}
