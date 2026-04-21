package mk.ukim.finki.konsultacii.security;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.konsultacii.repository.ConsultationAttendanceRepository;
import mk.ukim.finki.konsultacii.repository.ConsultationRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsultationSecurity {
    private final ConsultationRepository consultationRepository;

    public boolean isOwner(Long consultationId, String username) {
        return consultationRepository.findById(consultationId)
                .map(c -> c.getProfessor().getId().equals(username) || c.getProfessor().getEmail().equals(username))
                .orElse(false);
    }


}
