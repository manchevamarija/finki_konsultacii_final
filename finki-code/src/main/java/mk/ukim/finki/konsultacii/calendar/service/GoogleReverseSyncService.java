package mk.ukim.finki.konsultacii.calendar.service;

public interface GoogleReverseSyncService {
    void syncDeletedEventsForProfessor(String professorId);
}