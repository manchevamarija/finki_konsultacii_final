package mk.ukim.finki.konsultacii.model.projections;

import mk.ukim.finki.konsultacii.model.Room;

import java.time.DayOfWeek;
import java.time.LocalTime;


public interface RegularConsultationTermProjection {
    DayOfWeek getDayOfWeek();

    LocalTime getTimeFrom();

    LocalTime getTimeTo();

    Room getRoom();
}
