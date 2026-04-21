package mk.ukim.finki.konsultacii.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;


@Documented
@Constraint(validatedBy = TimeFromIsBeforeTimeToValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TimeFromIsBeforeTimeTo {
    String message() default "Времето на започнување мора да биде пред времето на завршување";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
