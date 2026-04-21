package mk.ukim.finki.konsultacii.model.exceptions;

public class IrregularConsultationTermNotFoundException extends RuntimeException{
    public IrregularConsultationTermNotFoundException(Long id) {
        super(String.format("Irregular consultation term with id %d was not found", id));
    }
}
