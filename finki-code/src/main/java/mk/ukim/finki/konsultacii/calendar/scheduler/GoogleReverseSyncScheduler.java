package mk.ukim.finki.konsultacii.calendar.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.calendar.service.GoogleReverseSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleReverseSyncScheduler {

    private final CalendarTokenService calendarTokenService;
    private final GoogleReverseSyncService googleReverseSyncService;
    private final CalendarReverseSyncTaskDispatcher taskDispatcher;

    @Scheduled(
            fixedDelayString = "${calendar.reverse-sync.fixed-delay-ms:10000}",
            initialDelayString = "${calendar.reverse-sync.initial-delay-ms:5000}"
    )
    public void reverseSyncGoogleCalendar() {
        List<String> connectedProfessors = calendarTokenService.findAllProfessorsWithGoogleConnected();
        log.debug("Google reverse sync: dispatching {} professors", connectedProfessors.size());
        taskDispatcher.dispatch("Google", connectedProfessors, googleReverseSyncService::syncDeletedEventsForProfessor);
    }
}
