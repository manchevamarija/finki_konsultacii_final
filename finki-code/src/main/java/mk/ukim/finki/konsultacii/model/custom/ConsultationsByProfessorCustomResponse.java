package mk.ukim.finki.konsultacii.model.custom;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.Professor;
import mk.ukim.finki.konsultacii.model.Room;

import java.io.Serializable;
import java.util.List;


@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultationsByProfessorCustomResponse implements Serializable {

    @JsonProperty("professor")
    private Professor professor;

    @JsonProperty("room")
    private Room room;

    @JsonProperty("regularConsultationTerms")
    private List<Consultation> regularConsultationTerms;

    @JsonProperty("irregularConsultationTerms")
    private List<Consultation> irregularConsultationTerms;
}
