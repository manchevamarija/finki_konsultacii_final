package mk.ukim.finki.konsultacii.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mk.ukim.finki.konsultacii.calendar.model.CalendarSyncStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;

import java.time.*;
import java.util.List;


@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
public class Consultation {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Professor professor;

    @ManyToOne
    private Room room;

    @Enumerated(EnumType.STRING)
    private ConsultationType type;

    private LocalDate oneTimeDate;

    @Enumerated(EnumType.STRING)
    private DayOfWeek weeklyDayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    @ElementCollection
    private List<LocalDate> canceledDates;

    @Enumerated(EnumType.STRING)
    private ConsultationStatus status;

    private Boolean online;

    private String studentInstructions;

    private String meetingLink;

    // Calendar integration fields
    @Column(name = "outlook_event_id", length = 512)
    private String outlookEventId;

    @Column(name = "google_calendar_event_id", length = 512)
    private String googleCalendarEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_sync_status")
    private CalendarSyncStatus calendarSyncStatus = CalendarSyncStatus.NOT_SYNCED;

    public Consultation(Professor professor, Room room, ConsultationType type,
                        LocalDate date, DayOfWeek dayOfWeek, LocalTime timeFrom, LocalTime timeTo,
                        Boolean online, String studentInstructions, String meetingLink) {
        this.professor = professor;
        this.room = room;
        this.type = type;
        this.oneTimeDate = date;
        this.weeklyDayOfWeek = dayOfWeek;
        this.startTime = timeFrom;
        this.endTime = timeTo;
        this.status = ConsultationStatus.ACTIVE;
        this.online = online;
        this.studentInstructions = studentInstructions;
        this.meetingLink = meetingLink;
    }

    public LocalDateTime getOneTimeStart() {
        return this.oneTimeDate.atTime(this.startTime);
    }


    public boolean isStarted() {
        ZoneId zoneId = ZoneId.of("Europe/Skopje");

        ZonedDateTime consultationStart = ZonedDateTime.of(this.oneTimeDate, this.startTime, zoneId);

        return consultationStart.isAfter(ZonedDateTime.now(zoneId));
    }

    public boolean isActive() {
        return this.status == ConsultationStatus.ACTIVE;
    }

    public boolean isEnded() {
        return LocalDateTime.now().isAfter(this.oneTimeDate.atTime(this.endTime));
    }

    public DayOfWeek getDayOfWeek() {
        return this.weeklyDayOfWeek;
    }

    public LocalTime getTimeFrom() {
        return this.startTime;
    }

    public LocalTime getTimeTo() {
        return this.endTime;
    }

    public String getTranslatedType(){
        if(this.type == ConsultationType.WEEKLY)
            return "Редовни";
        return "Дополнителни";
    }

    public boolean isMoreThanTwoHoursAway() {
        ZoneId zoneId = ZoneId.of("Europe/Skopje");

        ZonedDateTime consultationStart = ZonedDateTime.of(this.oneTimeDate, this.startTime, zoneId);

        ZonedDateTime nowPlusTwo = ZonedDateTime.now(zoneId).plusHours(2);

        return consultationStart.isAfter(nowPlusTwo);
    }

    public String getFormattedOneTimeDate() {
        return this.oneTimeDate.getDayOfMonth() + "." + this.oneTimeDate.getMonthValue() + "." + this.oneTimeDate.getYear();
    }
}

