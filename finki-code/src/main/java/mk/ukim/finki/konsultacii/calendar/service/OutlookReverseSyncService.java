package mk.ukim.finki.konsultacii.calendar.service;

public interface OutlookReverseSyncService {
    void syncDeletedEventsForProfessor(String professorId);
}