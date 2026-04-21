package mk.ukim.finki.konsultacii.model.custom;

import mk.ukim.finki.konsultacii.model.Room;

import java.time.LocalDate;
import java.time.LocalTime;


public interface ConsultationTermDto {
    Long getId();

    String getProfessorName();

    LocalDate getOneTimeDate();

    LocalTime getTimeFrom();

    LocalTime getTimeTo();

    Room getRoom();

    String getComment();
}
