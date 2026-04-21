package mk.ukim.finki.konsultacii.calendar.service;

import mk.ukim.finki.konsultacii.model.Consultation;

public interface CalendarSyncService {
    void onCreate(Consultation consultation, String professorId);
    void onUpdate(Consultation consultation, String professorId);
    void onDelete(Consultation consultation, String professorId);
    void onToggleStatus(Consultation consultation, boolean active, String professorId);
}