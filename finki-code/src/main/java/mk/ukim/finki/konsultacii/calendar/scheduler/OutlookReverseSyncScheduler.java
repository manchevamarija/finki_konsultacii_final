package mk.ukim.finki.konsultacii.calendar.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.calendar.service.OutlookReverseSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutlookReverseSyncScheduler {

    private final CalendarTokenService calendarTokenService;
    private final OutlookReverseSyncService outlookReverseSyncService;
    private final CalendarReverseSyncTaskDispatcher taskDispatcher;

    @Scheduled(
            fixedDelayString = "${calendar.reverse-sync.fixed-delay-ms:10000}",
            initialDelayString = "${calendar.reverse-sync.initial-delay-ms:5000}"
    )
    public void reverseSyncOutlookCalendar() {
        List<String> connectedProfessors = calendarTokenService.findAllProfessorsWithOutlookConnected();
        log.debug("Outlook reverse sync: dispatching {} professors", connectedProfessors.size());
        taskDispatcher.dispatch("Outlook", connectedProfessors, outlookReverseSyncService::syncDeletedEventsForProfessor);
    }
}
