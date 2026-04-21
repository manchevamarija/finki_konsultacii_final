package mk.ukim.finki.konsultacii.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import mk.ukim.finki.konsultacii.model.dtos.ConsultationFormDto;


public class RoomIdXORLinkIsNotNullValidator
        implements ConstraintValidator<RoomIdXORLinkIsNotNull, ConsultationFormDto> {

    @Override
    public boolean isValid(
            ConsultationFormDto consultationFormDto, ConstraintValidatorContext constraintValidatorContext) {
        return consultationFormDto.getRoomName() != null;
    }
}
