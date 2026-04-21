package mk.ukim.finki.konsultacii.model.dtos;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"professorId", "roomName", "oneTimeDate", "weeklyDayOfWeek", "startTime", "endTime", "online", "studentInstructions", "message"})
public class ConsultationDto {
    private String professorId;
    private String roomName;
    private String oneTimeDate;
    private String weeklyDayOfWeek;
    private String startTime;
    private String endTime;
    private String online;
    private String studentInstructions;
    private String message;
    private String meetingLink;
}
