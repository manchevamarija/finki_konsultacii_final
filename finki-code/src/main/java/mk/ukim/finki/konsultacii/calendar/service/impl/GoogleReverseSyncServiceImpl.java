package mk.ukim.finki.konsultacii.calendar.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.service.CalendarReverseSyncPropagationService;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.calendar.service.GoogleReverseSyncService;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.repository.ConsultationRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleReverseSyncServiceImpl implements GoogleReverseSyncService {

    private final ConsultationRepository consultationRepository;
    private final CalendarReverseSyncPropagationService reverseSyncPropagationService;
    private final GoogleCalendarServiceImpl googleCalendarService;
    private final CalendarTokenService calendarTokenService;

    @Override
    public void syncDeletedEventsForProfessor(String professorId) {
        if (!calendarTokenService.hasGoogleConnection(professorId)) {
            log.debug("Professor {} has no Google connection, skipping reverse sync", professorId);
            return;
        }

        List<Consultation> consultations = consultationRepository
                .findAllByProfessor_IdAndGoogleCalendarEventIdIsNotNullAndStatus(professorId, ConsultationStatus.ACTIVE);

        log.debug("Found {} active Google-synced consultations for professor {}", consultations.size(), professorId);

        for (Consultation consultation : consultations) {
            try {
                boolean exists = googleCalendarService.eventExists(professorId, consultation.getGoogleCalendarEventId());
                if (!exists) {
                    log.info("Google event {} was deleted; mirroring for consultation {}",
                            consultation.getGoogleCalendarEventId(), consultation.getId());
                    reverseSyncPropagationService.handleExternalGoogleDeletion(consultation);
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("Consultation {} changed in another transaction, skipping", consultation.getId());
            } catch (Exception e) {
                log.error("Failed checking Google event for consultation {}: {}", consultation.getId(), e.getMessage(), e);
            }
        }
    }
}