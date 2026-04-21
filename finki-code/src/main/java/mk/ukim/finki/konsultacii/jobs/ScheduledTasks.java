package mk.ukim.finki.konsultacii.jobs;

import mk.ukim.finki.konsultacii.service.RegularConsultationTermService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class ScheduledTasks {

    private final RegularConsultationTermService regularConsultationTermService;

    public ScheduledTasks(RegularConsultationTermService regularConsultationTermService) {
        this.regularConsultationTermService = regularConsultationTermService;
    }

}
