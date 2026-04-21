package mk.ukim.finki.konsultacii.model.exceptions;

public class RescheduleInactiveConsultationTermException extends RuntimeException {
    public RescheduleInactiveConsultationTermException() {
        super("Неактивни термини за консултации не може да бидат презакажани");
    }
}
