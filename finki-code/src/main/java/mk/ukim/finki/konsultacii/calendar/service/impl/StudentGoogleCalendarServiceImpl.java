package mk.ukim.finki.konsultacii.calendar.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.calendar.service.StudentGoogleCalendarService;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.ConsultationAttendance;
import mk.ukim.finki.konsultacii.repository.ConsultationAttendanceRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentGoogleCalendarServiceImpl implements StudentGoogleCalendarService {

    private static final String GCAL_BASE = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final String ZONE_ID = "Europe/Skopje";

    private final CalendarTokenService calendarTokenService;
    private final ConsultationAttendanceRepository attendanceRepository;
    private final RestTemplate restTemplate;

    @Override
    public void syncStudentAttendances(String studentIndex) {
        List<ConsultationAttendance> attendances = attendanceRepository.findByAttendee_Index(studentIndex);
        for (ConsultationAttendance attendance : attendances) {
            if (attendance.isActive() && !attendance.isEnded()) {
                syncOnAttendanceRegistered(attendance);
            }
        }
    }

    @Override
    public void syncOnAttendanceRegistered(ConsultationAttendance attendance) {
        String studentIndex = attendance.getAttendee().getIndex();
        if (!calendarTokenService.hasStudentGoogleConnection(studentIndex)) {
            log.info("Student {} has no connected Google Calendar; skipping direct student sync", studentIndex);
            return;
        }

        if (attendance.getStudentGoogleCalendarEventId() == null || attendance.getStudentGoogleCalendarEventId().isBlank()) {
            String eventId = createEvent(attendance);
            if (eventId != null) {
                attendance.setStudentGoogleCalendarEventId(eventId);
                attendanceRepository.save(attendance);
            }
            return;
        }

        updateEvent(attendance);
    }

    @Override
    public void syncAllAttendancesForConsultation(Consultation consultation, boolean deleteOnly) {
        List<ConsultationAttendance> attendances = attendanceRepository.findByConsultationId(consultation.getId());
        for (ConsultationAttendance attendance : attendances) {
            if (deleteOnly || !consultation.isActive()) {
                deleteEvent(attendance);
                continue;
            }
            syncOnAttendanceRegistered(attendance);
        }
    }

    @Override
    public void removeAttendanceEvent(ConsultationAttendance attendance) {
        deleteEvent(attendance);
    }

    private String createEvent(ConsultationAttendance attendance) {
        String accessToken = calendarTokenService.getValidStudentGoogleAccessToken(attendance.getAttendee().getIndex());
        if (accessToken == null) return null;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    GCAL_BASE,
                    HttpMethod.POST,
                    new HttpEntity<>(buildEventBody(attendance), buildHeaders(accessToken)),
                    Map.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String eventId = (String) response.getBody().get("id");
                log.info("Created student Google event {} for attendance {}", eventId, attendance.getId());
                return eventId;
            }
        } catch (Exception e) {
            log.error("Failed to create student Google event for attendance {}: {}", attendance.getId(), e.getMessage(), e);
        }
        return null;
    }

    private void updateEvent(ConsultationAttendance attendance) {
        String eventId = attendance.getStudentGoogleCalendarEventId();
        if (eventId == null || eventId.isBlank()) return;

        String accessToken = calendarTokenService.getValidStudentGoogleAccessToken(attendance.getAttendee().getIndex());
        if (accessToken == null) return;

        try {
            restTemplate.exchange(
                    GCAL_BASE + "/" + eventId,
                    HttpMethod.PUT,
                    new HttpEntity<>(buildEventBody(attendance), buildHeaders(accessToken)),
                    Map.class
            );
            log.info("Updated student Google event {} for attendance {}", eventId, attendance.getId());
        } catch (HttpClientErrorException.NotFound e) {
            attendance.setStudentGoogleCalendarEventId(null);
            attendanceRepository.save(attendance);
            syncOnAttendanceRegistered(attendance);
        } catch (Exception e) {
            log.error("Failed to update student Google event {} for attendance {}: {}", eventId, attendance.getId(), e.getMessage(), e);
        }
    }

    private void deleteEvent(ConsultationAttendance attendance) {
        String eventId = attendance.getStudentGoogleCalendarEventId();
        if (eventId == null || eventId.isBlank()) return;

        String accessToken = calendarTokenService.getValidStudentGoogleAccessToken(attendance.getAttendee().getIndex());
        if (accessToken == null) {
            attendance.setStudentGoogleCalendarEventId(null);
            attendanceRepository.save(attendance);
            return;
        }

        try {
            restTemplate.exchange(
                    GCAL_BASE + "/" + eventId,
                    HttpMethod.DELETE,
                    new HttpEntity<>(buildHeaders(accessToken)),
                    Void.class
            );
            log.info("Deleted student Google event {} for attendance {}", eventId, attendance.getId());
        } catch (HttpClientErrorException.NotFound ignored) {
            log.info("Student Google event {} was already deleted", eventId);
        } catch (Exception e) {
            log.error("Failed to delete student Google event {} for attendance {}: {}", eventId, attendance.getId(), e.getMessage(), e);
        } finally {
            attendance.setStudentGoogleCalendarEventId(null);
            attendanceRepository.save(attendance);
        }
    }

    private Map<String, Object> buildEventBody(ConsultationAttendance attendance) {
        Consultation consultation = attendance.getConsultation();
        ZoneId zone = ZoneId.of(ZONE_ID);
        ZonedDateTime start = ZonedDateTime.of(consultation.getOneTimeDate(), consultation.getStartTime(), zone);
        ZonedDateTime end = ZonedDateTime.of(consultation.getOneTimeDate(), consultation.getEndTime(), zone);
        DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        String title = "Консултации - " + consultation.getProfessor().getName();
        StringBuilder description = new StringBuilder("Тип: ").append(consultation.getTranslatedType());
        if (consultation.getStudentInstructions() != null && !consultation.getStudentInstructions().isBlank()) {
            description.append("\nНасоки: ").append(consultation.getStudentInstructions());
        }
        if (consultation.getMeetingLink() != null && !consultation.getMeetingLink().isBlank()) {
            description.append("\nЛинк: ").append(consultation.getMeetingLink());
        }

        String location = consultation.getRoom() != null ? consultation.getRoom().getName() : "";
        if (Boolean.TRUE.equals(consultation.getOnline())) {
            location += (location.isBlank() ? "" : ", ") + "Online";
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("summary", title);
        event.put("description", description.toString());
        event.put("location", location);
        event.put("status", consultation.isActive() ? "confirmed" : "cancelled");

        Map<String, String> startMap = new LinkedHashMap<>();
        startMap.put("dateTime", start.format(fmt));
        startMap.put("timeZone", ZONE_ID);
        event.put("start", startMap);

        Map<String, String> endMap = new LinkedHashMap<>();
        endMap.put("dateTime", end.format(fmt));
        endMap.put("timeZone", ZONE_ID);
        event.put("end", endMap);

        return event;
    }

    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}