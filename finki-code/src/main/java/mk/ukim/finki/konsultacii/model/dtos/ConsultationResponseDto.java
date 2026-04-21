package mk.ukim.finki.konsultacii.model.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ConsultationResponseDto {

    private String title;
    private String start;
    private String end;
    @JsonProperty("NumDeactivated")
    private int numDeactivated;
    @JsonProperty("Id")
    private Long id;
    @JsonProperty("TermType")
    private int termType;
    @JsonProperty("Day")
    private String day;
    @JsonProperty("DayNumber")
    private int dayNumber;
    @JsonProperty("Date")
    private String date;
    @JsonProperty("TimeFrom")
    private String timeFrom;
    @JsonProperty("TimeTo")
    private String timeTo;
    @JsonProperty("Classroom")
    private String classroom;
    @JsonProperty("Deactivated")
    private List<String> deactivated;
    @JsonProperty("DeactivatedStr")
    private List<String> deactivatedStr;
    private int checkedIn;
    @JsonProperty("CourseID")
    private Long courseID;
    private String dateFormatted;
    private String instructions;
}
