package mk.ukim.finki.konsultacii.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import mk.ukim.finki.konsultacii.model.dtos.ConsultationFormDto;

import java.time.LocalTime;


public class TimeFromIsBeforeTimeToValidator
        implements ConstraintValidator<TimeFromIsBeforeTimeTo, ConsultationFormDto> {

    @Override
    public boolean isValid(ConsultationFormDto consultationFormDto, ConstraintValidatorContext context) {
        LocalTime startTime = consultationFormDto.getStartTime();
        LocalTime endTime = consultationFormDto.getEndTime();

        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            String.format("Времето на започнување (%s) мора да биде пред времето на завршување (%s).",
                                    startTime, endTime))
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
