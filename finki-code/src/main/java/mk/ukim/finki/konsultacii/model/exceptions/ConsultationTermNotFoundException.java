package mk.ukim.finki.konsultacii.model.exceptions;

public class ConsultationTermNotFoundException extends RuntimeException {

    public ConsultationTermNotFoundException(Long id) {
        super(String.format("Consultation term with id %d not found", id));
    }
}
