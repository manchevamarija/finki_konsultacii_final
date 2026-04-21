package mk.ukim.finki.konsultacii.calendar.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.service.CalendarService;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.model.Consultation;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("googleCalendarService")
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarServiceImpl implements CalendarService {

    private static final String GCAL_BASE = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final String ZONE_ID = "Europe/Skopje";

    private final CalendarTokenService tokenService;
    private final RestTemplate restTemplate;

    @Override
    public boolean isConnected(String professorId) {
        return tokenService.hasGoogleConnection(professorId);
    }

    @Override
    public String createEvent(Consultation consultation, String professorId) {
        String accessToken = tokenService.getValidGoogleAccessToken(professorId);
        if (accessToken == null) {
            log.warn("No Google token for professor {}, skipping calendar sync", professorId);
            return null;
        }

        try {
            Map<String, Object> eventBody = buildEventBody(consultation, true);
            HttpHeaders headers = buildHeaders(accessToken);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GCAL_BASE,
                    HttpMethod.POST,
                    new HttpEntity<>(eventBody, headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String eventId = (String) response.getBody().get("id");
                log.info("Created Google Calendar event {} for consultation {}", eventId, consultation.getId());
                return eventId;
            }
        } catch (Exception e) {
            log.error("Failed to create Google Calendar event for consultation {}: {}",
                    consultation.getId(), e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void updateEvent(String eventId, Consultation consultation, String professorId) {
        if (eventId == null || eventId.isBlank()) return;

        String accessToken = tokenService.getValidGoogleAccessToken(professorId);
        if (accessToken == null) return;

        try {
            boolean isActive = consultation.isActive();
            Map<String, Object> eventBody = buildEventBody(consultation, isActive);

            restTemplate.exchange(
                    GCAL_BASE + "/" + eventId,
                    HttpMethod.PUT,
                    new HttpEntity<>(eventBody, buildHeaders(accessToken)),
                    Map.class
            );

            log.info("Updated Google Calendar event {} for consultation {}", eventId, consultation.getId());
        } catch (Exception e) {
            log.error("Failed to update Google Calendar event {} for consultation {}: {}",
                    eventId, consultation.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void deleteEvent(String eventId, String professorId) {
        if (eventId == null || eventId.isBlank()) return;

        String accessToken = tokenService.getValidGoogleAccessToken(professorId);
        if (accessToken == null) return;

        try {
            restTemplate.exchange(
                    GCAL_BASE + "/" + eventId,
                    HttpMethod.DELETE,
                    new HttpEntity<>(buildHeaders(accessToken)),
                    Void.class
            );
            log.info("Deleted Google Calendar event {}", eventId);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.GONE) {
                log.info("Google Calendar event {} was already deleted", eventId);
                return;
            }

            log.error("Failed to delete Google Calendar event {}: {}", eventId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to delete Google Calendar event {}: {}", eventId, e.getMessage(), e);
        }
    }

    @Override
    public void setEventStatus(String eventId, boolean active, String professorId) {
        if (eventId == null || eventId.isBlank()) return;

        String accessToken = tokenService.getValidGoogleAccessToken(professorId);
        if (accessToken == null) return;

        try {
            ResponseEntity<Map> getResp = restTemplate.exchange(
                    GCAL_BASE + "/" + eventId,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders(accessToken)),
                    Map.class
            );

            if (getResp.getBody() == null) return;

            @SuppressWarnings("unchecked")
            Map<String, Object> existingEvent = new LinkedHashMap<>(getResp.getBody());
            existingEvent.put("status", active ? "confirmed" : "cancelled");

            restTemplate.exchange(
                    GCAL_BASE + "/" + eventId,
                    HttpMethod.PUT,
                    new HttpEntity<>(existingEvent, buildHeaders(accessToken)),
                    Map.class
            );

            log.info("Set Google Calendar event {} status to active={}", eventId, active);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.GONE) {
                log.info("Cannot change status for Google event {} because it is already deleted", eventId);
                return;
            }

            log.error("Failed to set Google Calendar event {} status: {}", eventId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to set Google Calendar event {} status: {}", eventId, e.getMessage(), e);
        }
    }

    @Override
    public void addAttendeeToEvent(String eventId, String attendeeEmail, String attendeeName, String professorId) {
        if (eventId == null || eventId.isBlank() || attendeeEmail == null || attendeeEmail.isBlank()) return;

        String accessToken = tokenService.getValidGoogleAccessToken(professorId);
        if (accessToken == null) return;

        try {
            Map<String, Object> existingEvent = fetchEvent(eventId, accessToken);
            if (existingEvent == null) return;

            List<Map<String, Object>> attendees = extractGoogleAttendees(existingEvent.get("attendees"));
            if (containsGoogleAttendee(attendees, attendeeEmail)) {
                return;
            }

            Map<String, Object> attendee = new LinkedHashMap<>();
            attendee.put("email", attendeeEmail);
            if (attendeeName != null && !attendeeName.isBlank()) {
                attendee.put("displayName", attendeeName);
            }
            attendees.add(attendee);

            Map<String, Object> patchBody = new LinkedHashMap<>();
            patchBody.put("attendees", attendees);

            restTemplate.exchange(
                    GCAL_BASE + "/" + eventId + "?sendUpdates=all",
                    HttpMethod.PATCH,
                    new HttpEntity<>(patchBody, buildHeaders(accessToken)),
                    Map.class
            );

            log.info("Added attendee {} to Google event {}", attendeeEmail, eventId);
        } catch (Exception e) {
            log.error("Failed to add attendee {} to Google event {}: {}",
                    attendeeEmail, eventId, e.getMessage(), e);
        }
    }

    @Override
    public void removeAttendeeFromEvent(String eventId, String attendeeEmail, String professorId) {
        if (eventId == null || eventId.isBlank() || attendeeEmail == null || attendeeEmail.isBlank()) return;

        String accessToken = tokenService.getValidGoogleAccessToken(professorId);
        if (accessToken == null) return;

        try {
            Map<String, Object> existingEvent = fetchEvent(eventId, accessToken);
            if (existingEvent == null) return;

            List<Map<String, Object>> attendees = extractGoogleAttendees(existingEvent.get("attendees"));
            boolean removed = attendees.removeIf(attendee ->
                    attendeeEmail.equalsIgnoreCase(String.valueOf(attendee.get("email"))));

            if (!removed) {
                return;
            }

            Map<String, Object> patchBody = new LinkedHashMap<>();
            patchBody.put("attendees", attendees);

            restTemplate.exchange(
                    GCAL_BASE + "/" + eventId + "?sendUpdates=all",
                    HttpMethod.PATCH,
                    new HttpEntity<>(patchBody, buildHeaders(accessToken)),
                    Map.class
            );

            log.info("Removed attendee {} from Google event {}", attendeeEmail, eventId);
        } catch (Exception e) {
            log.error("Failed to remove attendee {} from Google event {}: {}",
                    attendeeEmail, eventId, e.getMessage(), e);
        }
    }

    public boolean eventExists(String professorId, String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }

        String accessToken = tokenService.getValidGoogleAccessToken(professorId);
        if (accessToken == null) {
            log.warn("No Google token for professor {}, cannot check event {}", professorId, eventId);
            return false;
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    GCAL_BASE + "/" + eventId,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders(accessToken)),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.info("Google event {} not found for professor {}", eventId, professorId);
                return false;
            }

            Object statusObj = response.getBody().get("status");
            String status = statusObj != null ? statusObj.toString() : null;
            boolean exists = status == null || !"cancelled".equalsIgnoreCase(status);

            log.info("Google event {} fetched for professor {}. Google status={}, exists={}",
                    eventId, professorId, status, exists);

            return exists;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.GONE) {
                log.info("Google event {} does not exist anymore for professor {}. statusCode={}",
                        eventId, professorId, e.getStatusCode().value());
                return false;
            }

            log.error("Google API error while checking event {} for professor {}: {}",
                    eventId, professorId, e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Unexpected error while checking Google event {} for professor {}: {}",
                    eventId, professorId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> buildEventBody(Consultation consultation, boolean isActive) {
        ZoneId zone = ZoneId.of(ZONE_ID);
        ZonedDateTime start = ZonedDateTime.of(consultation.getOneTimeDate(), consultation.getStartTime(), zone);
        ZonedDateTime end = ZonedDateTime.of(consultation.getOneTimeDate(), consultation.getEndTime(), zone);
        DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        String profName = consultation.getProfessor() != null
                ? consultation.getProfessor().getName()
                : "Професор";

        String title = "Консултации - " + profName;

        String description = "Тип: " + consultation.getTranslatedType();
        if (consultation.getStudentInstructions() != null && !consultation.getStudentInstructions().isBlank()) {
            description += "\nНасоки: " + consultation.getStudentInstructions();
        }
        if (consultation.getMeetingLink() != null && !consultation.getMeetingLink().isBlank()) {
            description += "\nЛинк: " + consultation.getMeetingLink();
        }

        String locationStr = "";
        if (consultation.getRoom() != null) {
            locationStr = consultation.getRoom().getName();
        }
        if (Boolean.TRUE.equals(consultation.getOnline())) {
            locationStr += (locationStr.isBlank() ? "" : ", ") + "Online";
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("summary", title);
        event.put("description", description);
        event.put("location", locationStr);
        event.put("status", isActive ? "confirmed" : "cancelled");

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

    private Map<String, Object> fetchEvent(String eventId, String accessToken) {
        ResponseEntity<Map> response = restTemplate.exchange(
                GCAL_BASE + "/" + eventId,
                HttpMethod.GET,
                new HttpEntity<>(buildHeaders(accessToken)),
                Map.class
        );

        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractGoogleAttendees(Object attendeesObj) {
        List<Map<String, Object>> attendees = new ArrayList<>();
        if (attendeesObj instanceof List<?> rawAttendees) {
            for (Object attendeeObj : rawAttendees) {
                if (attendeeObj instanceof Map<?, ?> attendeeMap) {
                    attendees.add(new LinkedHashMap<>((Map<String, Object>) attendeeMap));
                }
            }
        }
        return attendees;
    }

    private boolean containsGoogleAttendee(List<Map<String, Object>> attendees, String attendeeEmail) {
        for (Map<String, Object> attendee : attendees) {
            Object email = attendee.get("email");
            if (email != null && attendeeEmail.equalsIgnoreCase(email.toString())) {
                return true;
            }
        }
        return false;
    }
}
