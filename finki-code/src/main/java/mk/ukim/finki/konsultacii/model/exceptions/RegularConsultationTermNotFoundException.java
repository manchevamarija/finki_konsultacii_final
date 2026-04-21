package mk.ukim.finki.konsultacii.model.exceptions;

public class RegularConsultationTermNotFoundException extends RuntimeException {
    public RegularConsultationTermNotFoundException(Long id) {
        super(String.format("Regular consultation term with id %d was not found", id));
    }
}
