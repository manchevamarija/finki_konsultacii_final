package mk.ukim.finki.konsultacii.calendar.service;

import mk.ukim.finki.konsultacii.model.Consultation;

public interface CalendarReverseSyncPropagationService {
    void handleExternalGoogleDeletion(Consultation consultation);
    void handleExternalOutlookDeletion(Consultation consultation);
}