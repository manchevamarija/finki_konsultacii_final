package mk.ukim.finki.konsultacii.calendar.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.service.CalendarReverseSyncPropagationService;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.calendar.service.OutlookReverseSyncService;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.repository.ConsultationRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutlookReverseSyncServiceImpl implements OutlookReverseSyncService {

    private final ConsultationRepository consultationRepository;
    private final OutlookCalendarServiceImpl outlookCalendarService;
    private final CalendarReverseSyncPropagationService reverseSyncPropagationService;
    private final CalendarTokenService calendarTokenService;

    @Override
    public void syncDeletedEventsForProfessor(String professorId) {
        if (!calendarTokenService.hasOutlookConnection(professorId)) {
            log.debug("Professor {} has no Outlook connection, skipping reverse sync", professorId);
            return;
        }

        List<Consultation> consultations = consultationRepository
                .findAllByProfessor_IdAndOutlookEventIdIsNotNullAndStatus(professorId, ConsultationStatus.ACTIVE);

        log.debug("Found {} active Outlook-synced consultations for professor {}", consultations.size(), professorId);

        for (Consultation consultation : consultations) {
            try {
                boolean exists = outlookCalendarService.eventExists(consultation.getOutlookEventId(), professorId);
                if (!exists) {
                    reverseSyncPropagationService.handleExternalOutlookDeletion(consultation);
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("Consultation {} changed in another transaction, skipping", consultation.getId());
            } catch (Exception e) {
                log.error("Failed checking Outlook event for consultation {}: {}", consultation.getId(), e.getMessage(), e);
            }
        }
    }
}