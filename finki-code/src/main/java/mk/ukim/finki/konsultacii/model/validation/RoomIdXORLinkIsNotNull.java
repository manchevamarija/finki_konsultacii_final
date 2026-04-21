package mk.ukim.finki.konsultacii.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;


@Documented
@Constraint(validatedBy = RoomIdXORLinkIsNotNullValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RoomIdXORLinkIsNotNull {
    String message() default "Полето за просторија не може да биде празно.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
