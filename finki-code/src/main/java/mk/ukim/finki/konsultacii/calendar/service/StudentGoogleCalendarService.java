package mk.ukim.finki.konsultacii.calendar.service;

import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.ConsultationAttendance;

public interface StudentGoogleCalendarService {
    void syncStudentAttendances(String studentIndex);
    void syncOnAttendanceRegistered(ConsultationAttendance attendance);
    void syncAllAttendancesForConsultation(Consultation consultation, boolean deleteOnly);
    void removeAttendanceEvent(ConsultationAttendance attendance);
}