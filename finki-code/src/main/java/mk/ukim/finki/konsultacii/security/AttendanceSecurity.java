package mk.ukim.finki.konsultacii.security;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.konsultacii.model.ConsultationAttendance;
import mk.ukim.finki.konsultacii.repository.ConsultationAttendanceRepository;
import mk.ukim.finki.konsultacii.repository.StudentRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceSecurity {
    private final ConsultationAttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;

    public boolean isOwner(Long attendanceId, String username) {
        return attendanceRepository.findById(attendanceId)
                .map(att -> {
                    String index = att.getAttendee().getIndex();
                    if (index.equals(username)) return true;
                    if (att.getAttendee().hasEmail(username)) return true;
                    return studentRepository.findById(index)
                            .map(s -> s.hasEmail(username) ||
                                    (s.getEmail() != null && s.getEmail().toLowerCase().startsWith(username.toLowerCase() + "@")))
                            .orElse(false);
                })
                .orElse(false);
    }

    public boolean isProfessor(Long attendanceId, String username) {
        return attendanceRepository.findById(attendanceId)
                .map(att -> att.getConsultation().getProfessor().getId().equals(username))
                .orElse(false);
    }
}