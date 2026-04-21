package mk.ukim.finki.konsultacii.model.views;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import mk.ukim.finki.konsultacii.model.Professor;
import mk.ukim.finki.konsultacii.model.Room;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import org.hibernate.annotations.Immutable;

import java.time.*;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "consultation_view")
@Immutable
public class ConsultationView {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_name")
    private Room room;

    @Enumerated(EnumType.STRING)
    private ConsultationType type;

    private LocalDate oneTimeDate;

    @Enumerated(EnumType.STRING)
    private DayOfWeek weeklyDayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private ConsultationStatus status;

    private Boolean online;

    private String studentInstructions;

    private String meetingLink;

    @Column(name = "attendance_count")
    private Long attendanceCount;

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