package mk.ukim.finki.konsultacii.calendar.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.model.CalendarSyncStatus;
import mk.ukim.finki.konsultacii.calendar.service.CalendarReverseSyncPropagationService;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.repository.ConsultationRepository;
import mk.ukim.finki.konsultacii.service.SseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarReverseSyncPropagationServiceImpl implements CalendarReverseSyncPropagationService {

    private final ConsultationRepository consultationRepository;
    private final CalendarTokenService calendarTokenService;
    private final GoogleCalendarServiceImpl googleCalendarService;
    private final OutlookCalendarServiceImpl outlookCalendarService;
    private final SseService sseService;
    private final Map<Long, Object> consultationLocks = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void handleExternalGoogleDeletion(Consultation consultation) {
        Object lock = consultationLocks.computeIfAbsent(consultation.getId(), id -> new Object());

        synchronized (lock) {
            Consultation currentConsultation = findActiveConsultation(consultation.getId(), "Google");
            if (currentConsultation == null) {
                return;
            }

            handleExternalGoogleDeletionLocked(currentConsultation);
        }
    }

    private void handleExternalGoogleDeletionLocked(Consultation consultation) {
        String professorId = consultation.getProfessor().getId();
        boolean propagationFailed = false;

        if (hasText(consultation.getOutlookEventId())) {
            if (calendarTokenService.hasOutlookConnection(professorId)) {
                try {
                    outlookCalendarService.deleteEvent(consultation.getOutlookEventId(), professorId);
                    log.info("Deleted matching Outlook event for consultation {} after Google deletion", consultation.getId());
                    consultation.setOutlookEventId(null);
                } catch (Exception e) {
                    propagationFailed = true;
                    log.error("Failed to delete matching Outlook event for consultation {}: {}", consultation.getId(), e.getMessage(), e);
                }
            } else {
                propagationFailed = true;
                log.warn("Outlook not connected for professor {}, cannot mirror Google deletion for consultation {}", professorId, consultation.getId());
            }
        }

        consultation.setGoogleCalendarEventId(null);
        finalizeExternalDeletion(consultation, propagationFailed, "Google");
    }

    @Override
    @Transactional
    public void handleExternalOutlookDeletion(Consultation consultation) {
        Object lock = consultationLocks.computeIfAbsent(consultation.getId(), id -> new Object());

        synchronized (lock) {
            Consultation currentConsultation = findActiveConsultation(consultation.getId(), "Outlook");
            if (currentConsultation == null) {
                return;
            }

            handleExternalOutlookDeletionLocked(currentConsultation);
        }
    }

    private void handleExternalOutlookDeletionLocked(Consultation consultation) {
        String professorId = consultation.getProfessor().getId();
        boolean propagationFailed = false;

        if (hasText(consultation.getGoogleCalendarEventId())) {
            if (calendarTokenService.hasGoogleConnection(professorId)) {
                try {
                    googleCalendarService.deleteEvent(consultation.getGoogleCalendarEventId(), professorId);
                    log.info("Deleted matching Google event for consultation {} after Outlook deletion", consultation.getId());
                    consultation.setGoogleCalendarEventId(null);
                } catch (Exception e) {
                    propagationFailed = true;
                    log.error("Failed to delete matching Google event for consultation {}: {}", consultation.getId(), e.getMessage(), e);
                }
            } else {
                propagationFailed = true;
                log.warn("Google not connected for professor {}, cannot mirror Outlook deletion for consultation {}", professorId, consultation.getId());
            }
        }

        consultation.setOutlookEventId(null);
        finalizeExternalDeletion(consultation, propagationFailed, "Outlook");
    }

    private void finalizeExternalDeletion(Consultation consultation, boolean propagationFailed, String source) {
        consultation.setStatus(ConsultationStatus.INACTIVE);

        boolean stillSyncedSomewhere = hasText(consultation.getGoogleCalendarEventId())
                || hasText(consultation.getOutlookEventId());

        if (propagationFailed && stillSyncedSomewhere) {
            consultation.setCalendarSyncStatus(CalendarSyncStatus.SYNC_FAILED);
        } else {
            consultation.setCalendarSyncStatus(stillSyncedSomewhere
                    ? CalendarSyncStatus.SYNCED
                    : CalendarSyncStatus.NOT_SYNCED);
        }

        consultationRepository.save(consultation);
        sseService.sendConsultationUpdate();

        log.info("Consultation {} set to INACTIVE after {} calendar deletion. googleEventId={}, outlookEventId={}, syncStatus={}",
                consultation.getId(), source, consultation.getGoogleCalendarEventId(),
                consultation.getOutlookEventId(), consultation.getCalendarSyncStatus());
    }

    private Consultation findActiveConsultation(Long consultationId, String source) {
        Consultation consultation = consultationRepository.findById(consultationId).orElse(null);

        if (consultation == null) {
            log.warn("Consultation {} was not found during {} reverse sync", consultationId, source);
            return null;
        }

        if (consultation.getStatus() != ConsultationStatus.ACTIVE) {
            log.debug("Consultation {} is already {}, skipping {} reverse sync",
                    consultationId, consultation.getStatus(), source);
            return null;
        }

        return consultation;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
