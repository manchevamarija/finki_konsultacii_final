package mk.ukim.finki.konsultacii.calendar.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.service.CalendarService;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.model.Consultation;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Microsoft Graph API calendar integration.
 * Docs: https://learn.microsoft.com/en-us/graph/api/resources/event
 */
@Service("outlookCalendarService")
@RequiredArgsConstructor
@Slf4j
public class OutlookCalendarServiceImpl implements CalendarService {

    private static final String GRAPH_BASE = "https://graph.microsoft.com/v1.0";
    private static final String GRAPH_EVENTS_BASE = GRAPH_BASE + "/me/events";
    private static final ZoneId APP_ZONE = ZoneId.of("Europe/Skopje");
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final String INACTIVE_PREFIX = "[НЕАКТИВНА] ";
    private static final DateTimeFormatter OUTLOOK_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final CalendarTokenService tokenService;
    private final RestTemplate restTemplate;

    @Override
    public boolean isConnected(String professorId) {
        return tokenService.hasOutlookConnection(professorId);
    }

    @Override
    public String createEvent(Consultation consultation, String professorId) {
        String accessToken = tokenService.getValidOutlookAccessToken(professorId);
        if (accessToken == null) {
            log.warn("No Outlook token for professor {}, skipping calendar sync", professorId);
            return null;
        }

        try {
            Map<String, Object> eventBody = buildEventBody(consultation, consultation.isActive());
            HttpHeaders headers = buildHeaders(accessToken);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GRAPH_EVENTS_BASE,
                    HttpMethod.POST,
                    new HttpEntity<>(eventBody, headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String eventId = (String) response.getBody().get("id");
                log.info("Created Outlook event {} for consultation {}", eventId, consultation.getId());
                return eventId;
            }
        } catch (Exception e) {
            log.error("Failed to create Outlook event for consultation {}: {}",
                    consultation.getId(), e.getMessage(), e);
        }

        return null;
    }

    @Override
    public void updateEvent(String eventId, Consultation consultation, String professorId) {
        if (eventId == null || eventId.isBlank()) return;

        String accessToken = tokenService.getValidOutlookAccessToken(professorId);
        if (accessToken == null) return;

        try {
            Map<String, Object> eventBody = buildEventBody(consultation, consultation.isActive());
            String encodedEventId = encodeEventId(eventId);

            restTemplate.exchange(
                    GRAPH_EVENTS_BASE + "/" + encodedEventId,
                    HttpMethod.PATCH,
                    new HttpEntity<>(eventBody, buildHeaders(accessToken)),
                    Map.class
            );

            log.info("Updated Outlook event {} for consultation {}", eventId, consultation.getId());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.GONE) {
                log.info("Outlook event {} does not exist anymore, will need re-create for consultation {}",
                        eventId, consultation.getId());
                return;
            }

            log.error("Failed to update Outlook event {} for consultation {}: {}",
                    eventId, consultation.getId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to update Outlook event {} for consultation {}: {}",
                    eventId, consultation.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void deleteEvent(String eventId, String professorId) {
        if (eventId == null || eventId.isBlank()) return;

        String accessToken = tokenService.getValidOutlookAccessToken(professorId);
        if (accessToken == null) return;

        try {
            String encodedEventId = encodeEventId(eventId);

            restTemplate.exchange(
                    GRAPH_EVENTS_BASE + "/" + encodedEventId,
                    HttpMethod.DELETE,
                    new HttpEntity<>(buildHeaders(accessToken)),
                    Void.class
            );

            log.info("Deleted Outlook event {}", eventId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.GONE) {
                log.info("Outlook event {} was already deleted", eventId);
                return;
            }

            log.error("Failed to delete Outlook event {}: {}", eventId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to delete Outlook event {}: {}", eventId, e.getMessage(), e);
        }
    }

    @Override
    public void setEventStatus(String eventId, boolean active, String professorId) {
        if (eventId == null || eventId.isBlank()) return;

        String accessToken = tokenService.getValidOutlookAccessToken(professorId);
        if (accessToken == null) return;

        try {
            String encodedEventId = encodeEventId(eventId);

            ResponseEntity<Map> getResponse = restTemplate.exchange(
                    GRAPH_EVENTS_BASE + "/" + encodedEventId + "?$select=subject",
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders(accessToken)),
                    Map.class
            );

            String currentSubject = "";
            if (getResponse.getBody() != null) {
                currentSubject = (String) getResponse.getBody().getOrDefault("subject", "");
            }

            if (currentSubject.startsWith(INACTIVE_PREFIX)) {
                currentSubject = currentSubject.substring(INACTIVE_PREFIX.length());
            }

            String newSubject = active ? currentSubject : INACTIVE_PREFIX + currentSubject;

            Map<String, Object> patch = new LinkedHashMap<>();
            patch.put("subject", newSubject);
            patch.put("showAs", active ? "busy" : "free");

            restTemplate.exchange(
                    GRAPH_EVENTS_BASE + "/" + encodedEventId,
                    HttpMethod.PATCH,
                    new HttpEntity<>(patch, buildHeaders(accessToken)),
                    Map.class
            );

            log.info("Set Outlook event {} status to active={}", eventId, active);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.GONE) {
                log.info("Cannot change status for Outlook event {} because it is already deleted", eventId);
                return;
            }

            log.error("Failed to set Outlook event {} status: {}", eventId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to set Outlook event {} status: {}", eventId, e.getMessage(), e);
        }
    }

    @Override
    public void addAttendeeToEvent(String eventId, String attendeeEmail, String attendeeName, String professorId) {
        if (eventId == null || eventId.isBlank() || attendeeEmail == null || attendeeEmail.isBlank()) return;

        String accessToken = tokenService.getValidOutlookAccessToken(professorId);
        if (accessToken == null) return;

        try {
            String encodedEventId = encodeEventId(eventId);
            Map<String, Object> existingEvent = fetchEvent(encodedEventId, accessToken);
            if (existingEvent == null) return;

            List<Map<String, Object>> attendees = extractOutlookAttendees(existingEvent.get("attendees"));
            if (containsOutlookAttendee(attendees, attendeeEmail)) {
                return;
            }

            attendees.add(buildOutlookAttendee(attendeeEmail, attendeeName));

            Map<String, Object> patch = new LinkedHashMap<>();
            patch.put("attendees", attendees);

            restTemplate.exchange(
                    GRAPH_EVENTS_BASE + "/" + encodedEventId,
                    HttpMethod.PATCH,
                    new HttpEntity<>(patch, buildHeaders(accessToken)),
                    Map.class
            );

            log.info("Added attendee {} to Outlook event {}", attendeeEmail, eventId);
        } catch (HttpClientErrorException e) {
            if (isMissingEvent(e)) {
                log.warn("Outlook event {} was not found while adding attendee {}. The stored event id is stale.",
                        eventId, attendeeEmail);
                return;
            }

            log.error("Failed to add attendee {} to Outlook event {}: {}",
                    attendeeEmail, eventId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to add attendee {} to Outlook event {}: {}",
                    attendeeEmail, eventId, e.getMessage(), e);
        }
    }

    @Override
    public void removeAttendeeFromEvent(String eventId, String attendeeEmail, String professorId) {
        if (eventId == null || eventId.isBlank() || attendeeEmail == null || attendeeEmail.isBlank()) return;

        String accessToken = tokenService.getValidOutlookAccessToken(professorId);
        if (accessToken == null) return;

        try {
            String encodedEventId = encodeEventId(eventId);
            Map<String, Object> existingEvent = fetchEvent(encodedEventId, accessToken);
            if (existingEvent == null) return;

            List<Map<String, Object>> attendees = extractOutlookAttendees(existingEvent.get("attendees"));
            boolean removed = attendees.removeIf(attendee -> {
                Object emailAddressObj = attendee.get("emailAddress");
                if (!(emailAddressObj instanceof Map<?, ?> emailAddress)) {
                    return false;
                }

                Object address = emailAddress.get("address");
                return address != null && attendeeEmail.equalsIgnoreCase(address.toString());
            });

            if (!removed) {
                return;
            }

            Map<String, Object> patch = new LinkedHashMap<>();
            patch.put("attendees", attendees);

            restTemplate.exchange(
                    GRAPH_EVENTS_BASE + "/" + encodedEventId,
                    HttpMethod.PATCH,
                    new HttpEntity<>(patch, buildHeaders(accessToken)),
                    Map.class
            );

            log.info("Removed attendee {} from Outlook event {}", attendeeEmail, eventId);
        } catch (HttpClientErrorException e) {
            if (isMissingEvent(e)) {
                log.warn("Outlook event {} was not found while removing attendee {}. The stored event id is stale.",
                        eventId, attendeeEmail);
                return;
            }

            log.error("Failed to remove attendee {} from Outlook event {}: {}",
                    attendeeEmail, eventId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to remove attendee {} from Outlook event {}: {}",
                    attendeeEmail, eventId, e.getMessage(), e);
        }
    }

    public boolean eventExists(String eventId, String professorId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }

        String accessToken = tokenService.getValidOutlookAccessToken(professorId);
        if (accessToken == null) {
            log.warn("No Outlook token for professor {}, cannot check event {}", professorId, eventId);
            return true;
        }

        try {
            String encodedEventId = encodeEventId(eventId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GRAPH_EVENTS_BASE + "/" + encodedEventId,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders(accessToken)),
                    Map.class
            );

            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.GONE) {
                log.info("Outlook event {} does not exist anymore for professor {}", eventId, professorId);
                return false;
            }

            log.error("Outlook API error while checking event {} for professor {}: {}",
                    eventId, professorId, e.getMessage(), e);
            return true;
        } catch (Exception e) {
            log.error("Unexpected error while checking Outlook event {} for professor {}: {}",
                    eventId, professorId, e.getMessage(), e);
            return true;
        }
    }

    private Map<String, Object> buildEventBody(Consultation consultation, boolean isActive) {
        ZonedDateTime start = ZonedDateTime.of(
                consultation.getOneTimeDate(),
                consultation.getStartTime(),
                APP_ZONE
        ).withZoneSameInstant(UTC_ZONE);

        ZonedDateTime end = ZonedDateTime.of(
                consultation.getOneTimeDate(),
                consultation.getEndTime(),
                APP_ZONE
        ).withZoneSameInstant(UTC_ZONE);

        String title = buildTitle(consultation);
        if (!isActive) {
            title = INACTIVE_PREFIX + title;
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("subject", title);
        event.put("showAs", isActive ? "busy" : "free");

        String locationName = consultation.getRoom() != null ? consultation.getRoom().getName() : "";
        if (Boolean.TRUE.equals(consultation.getOnline())) {
            locationName += (locationName.isBlank() ? "" : ", ") + "Online";
        }
        if (!locationName.isBlank()) {
            Map<String, Object> location = new LinkedHashMap<>();
            location.put("displayName", locationName);
            event.put("location", location);
        }

        StringBuilder bodyText = new StringBuilder("Консултации - ")
                .append(escapeHtml(consultation.getTranslatedType()));

        if (consultation.getStudentInstructions() != null && !consultation.getStudentInstructions().isBlank()) {
            bodyText.append("<br/>Насоки за студенти: ")
                    .append(escapeHtml(consultation.getStudentInstructions()));
        }

        if (consultation.getMeetingLink() != null && !consultation.getMeetingLink().isBlank()) {
            bodyText.append("<br/>Линк: ")
                    .append(escapeHtml(consultation.getMeetingLink()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contentType", "HTML");
        body.put("content", bodyText.toString());
        event.put("body", body);

        Map<String, Object> startMap = new LinkedHashMap<>();
        startMap.put("dateTime", start.toLocalDateTime().format(OUTLOOK_DATE_TIME));
        startMap.put("timeZone", "UTC");
        event.put("start", startMap);

        Map<String, Object> endMap = new LinkedHashMap<>();
        endMap.put("dateTime", end.toLocalDateTime().format(OUTLOOK_DATE_TIME));
        endMap.put("timeZone", "UTC");
        event.put("end", endMap);

        return event;
    }

    private String buildTitle(Consultation consultation) {
        String profName = consultation.getProfessor() != null
                ? consultation.getProfessor().getName()
                : "Професор";
        return "Консултации - " + profName;
    }

    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String encodeEventId(String eventId) {
        return UriUtils.encodePathSegment(eventId, StandardCharsets.UTF_8);
    }

    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private Map<String, Object> fetchEvent(String encodedEventId, String accessToken) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    GRAPH_EVENTS_BASE + "/" + encodedEventId,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders(accessToken)),
                    Map.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (isMissingEvent(e)) {
                log.warn("Outlook event {} was not found. Skipping attendee sync for this stale event id.",
                        encodedEventId);
                return null;
            }

            throw e;
        }
    }

    private boolean isMissingEvent(HttpClientErrorException e) {
        return e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.GONE;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractOutlookAttendees(Object attendeesObj) {
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

    private boolean containsOutlookAttendee(List<Map<String, Object>> attendees, String attendeeEmail) {
        for (Map<String, Object> attendee : attendees) {
            Object emailAddressObj = attendee.get("emailAddress");
            if (!(emailAddressObj instanceof Map<?, ?> emailAddress)) {
                continue;
            }

            Object address = emailAddress.get("address");
            if (address != null && attendeeEmail.equalsIgnoreCase(address.toString())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> buildOutlookAttendee(String attendeeEmail, String attendeeName) {
        Map<String, Object> emailAddress = new LinkedHashMap<>();
        emailAddress.put("address", attendeeEmail);
        if (attendeeName != null && !attendeeName.isBlank()) {
            emailAddress.put("name", attendeeName);
        }

        Map<String, Object> attendee = new LinkedHashMap<>();
        attendee.put("emailAddress", emailAddress);
        attendee.put("type", "required");
        return attendee;
    }
}
