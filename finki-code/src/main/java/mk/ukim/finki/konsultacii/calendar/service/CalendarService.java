package mk.ukim.finki.konsultacii.calendar.service;

import mk.ukim.finki.konsultacii.model.Consultation;

/**
 * Calendar integration service.
 * Each implementation handles one provider (Outlook or Google).
 */
public interface CalendarService {

    /**
     * Creates a calendar event for the given consultation.
     * @return the external event ID, or null if creation failed
     */
    String createEvent(Consultation consultation, String professorId);

    /**
     * Updates an existing calendar event to reflect changes in the consultation.
     */
    void updateEvent(String eventId, Consultation consultation, String professorId);

    /**
     * Permanently deletes a calendar event.
     */
    void deleteEvent(String eventId, String professorId);

    /**
     * Activates or deactivates an event.
     * For Outlook: prefixes title with [НЕАКТИВНА] when inactive.
     * For Google: sets status to "cancelled" / "confirmed".
     */
    void setEventStatus(String eventId, boolean active, String professorId);

    /**
     * Adds a student as an attendee to an existing professor event.
     */
    void addAttendeeToEvent(String eventId, String attendeeEmail, String attendeeName, String professorId);

    /**
     * Removes a student attendee from an existing professor event.
     */
    void removeAttendeeFromEvent(String eventId, String attendeeEmail, String professorId);

    /**
     * Returns true if the professor has a valid connected token for this provider.
     */
    boolean isConnected(String professorId);
}
