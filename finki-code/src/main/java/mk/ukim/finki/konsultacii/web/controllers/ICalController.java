package mk.ukim.finki.konsultacii.web.controllers;

import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.dtos.StudentScheduledConsultationDto;
import mk.ukim.finki.konsultacii.model.enumerations.RoomType;
import mk.ukim.finki.konsultacii.service.ConsultationAttendanceService;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Location;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


@Controller
@AllArgsConstructor
public class ICalController {

    private final ConsultationAttendanceService consultationAttendanceService;

    @GetMapping(path = "/generate-calendar")
    public ResponseEntity<Resource> generateCalenderFile(Authentication authentication) {

        Calendar icsCalendar = new Calendar();
        List<StudentScheduledConsultationDto> studentScheduledConsultationDtoList = this.consultationAttendanceService
                .findAllFutureActiveConsultationTermsForStudent(authentication.getName());

        studentScheduledConsultationDtoList
                .forEach(s -> {
                    String eventSummary = "Консултации кај: " + s.getProfessorName();
                    Location location = new Location();
                    VEvent event;
                    LocalDate date = s.getOneTimeDate();
                    LocalTime startTime = s.getStartTime();
                    LocalTime endTime = s.getEndTime();

                    LocalDateTime start = LocalDateTime.of(date, startTime);
                    LocalDateTime end = LocalDateTime.of(date, endTime);
                    event = new VEvent(start, end, eventSummary);
                    if (s.getRoom().getType().equals(RoomType.VIRTUAL))
                        location.setValue(s.getRoom().getLocationDescription());
                    else
                        location.setValue(s.getRoom().getName());

                    Attendee attendee = new Attendee();
                    attendee.setValue(authentication.getName());
                    event.add(attendee);

                    event.add(location);
                    icsCalendar.add(event);
                });


        byte[] calendarByte = icsCalendar.toString().getBytes();
        Resource resource = new ByteArrayResource(calendarByte);

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=calendar.ics");
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        return ResponseEntity.ok().headers(header).contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }
}
