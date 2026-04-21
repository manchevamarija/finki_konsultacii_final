package mk.ukim.finki.konsultacii.calendar.service.impl;

import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.model.CalendarSyncStatus;
import mk.ukim.finki.konsultacii.calendar.service.CalendarService;
import mk.ukim.finki.konsultacii.calendar.service.CalendarSyncService;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.repository.ConsultationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CalendarSyncServiceImpl implements CalendarSyncService {

    private final CalendarService outlookService;
    private final CalendarService googleService;
    private final ConsultationRepository consultationRepository;

    public CalendarSyncServiceImpl(
            @Qualifier("outlookCalendarService") CalendarService outlookService,
            @Qualifier("googleCalendarService") CalendarService googleService,
            ConsultationRepository consultationRepository
    ) {
        this.outlookService = outlookService;
        this.googleService = googleService;
        this.consultationRepository = consultationRepository;
    }

    @Override
    public void onCreate(Consultation consultation, String professorId) {
        if (outlookService.isConnected(professorId)) {
            String eventId = outlookService.createEvent(consultation, professorId);
            if (hasText(eventId)) consultation.setOutlookEventId(eventId);
        }
        if (googleService.isConnected(professorId)) {
            String eventId = googleService.createEvent(consultation, professorId);
            if (hasText(eventId)) consultation.setGoogleCalendarEventId(eventId);
        }
        refreshSyncStatus(consultation, professorId);
        consultationRepository.save(consultation);
    }

    @Override
    public void onUpdate(Consultation consultation, String professorId) {
        if (outlookService.isConnected(professorId)) {
            if (hasText(consultation.getOutlookEventId())) {
                outlookService.updateEvent(consultation.getOutlookEventId(), consultation, professorId);
            } else {
                String eventId = outlookService.createEvent(consultation, professorId);
                if (hasText(eventId)) consultation.setOutlookEventId(eventId);
            }
        }
        if (googleService.isConnected(professorId)) {
            if (hasText(consultation.getGoogleCalendarEventId())) {
                googleService.updateEvent(consultation.getGoogleCalendarEventId(), consultation, professorId);
            } else {
                String eventId = googleService.createEvent(consultation, professorId);
                if (hasText(eventId)) consultation.setGoogleCalendarEventId(eventId);
            }
        }
        refreshSyncStatus(consultation, professorId);
        consultationRepository.save(consultation);
    }

    @Override
    public void onDelete(Consultation consultation, String professorId) {
        if (outlookService.isConnected(professorId) && hasText(consultation.getOutlookEventId())) {
            outlookService.deleteEvent(consultation.getOutlookEventId(), professorId);
            consultation.setOutlookEventId(null);
        }
        if (googleService.isConnected(professorId) && hasText(consultation.getGoogleCalendarEventId())) {
            googleService.deleteEvent(consultation.getGoogleCalendarEventId(), professorId);
            consultation.setGoogleCalendarEventId(null);
        }
        consultation.setCalendarSyncStatus(CalendarSyncStatus.NOT_SYNCED);
        consultationRepository.save(consultation);
    }

    @Override
    public void onToggleStatus(Consultation consultation, boolean active, String professorId) {
        if (active) {
            if (outlookService.isConnected(professorId)) {
                if (hasText(consultation.getOutlookEventId())) {
                    outlookService.updateEvent(consultation.getOutlookEventId(), consultation, professorId);
                } else {
                    String newEventId = outlookService.createEvent(consultation, professorId);
                    if (hasText(newEventId)) consultation.setOutlookEventId(newEventId);
                }
            }
            if (googleService.isConnected(professorId)) {
                if (hasText(consultation.getGoogleCalendarEventId())) {
                    googleService.setEventStatus(consultation.getGoogleCalendarEventId(), true, professorId);
                } else {
                    String newEventId = googleService.createEvent(consultation, professorId);
                    if (hasText(newEventId)) consultation.setGoogleCalendarEventId(newEventId);
                }
            }
        } else {
            if (outlookService.isConnected(professorId) && hasText(consultation.getOutlookEventId())) {
                outlookService.deleteEvent(consultation.getOutlookEventId(), professorId);
                consultation.setOutlookEventId(null);
            }
            if (googleService.isConnected(professorId) && hasText(consultation.getGoogleCalendarEventId())) {
                googleService.setEventStatus(consultation.getGoogleCalendarEventId(), false, professorId);
            }
        }
        refreshSyncStatus(consultation, professorId);
        consultationRepository.save(consultation);
    }

    private void refreshSyncStatus(Consultation consultation, String professorId) {
        boolean synced = (outlookService.isConnected(professorId) && hasText(consultation.getOutlookEventId()))
                || (googleService.isConnected(professorId) && hasText(consultation.getGoogleCalendarEventId()));
        consultation.setCalendarSyncStatus(synced ? CalendarSyncStatus.SYNCED : CalendarSyncStatus.NOT_SYNCED);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}