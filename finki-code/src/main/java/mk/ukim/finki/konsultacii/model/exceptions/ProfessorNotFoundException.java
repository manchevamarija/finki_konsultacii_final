package mk.ukim.finki.konsultacii.model.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProfessorNotFoundException extends RuntimeException {

    public ProfessorNotFoundException() {
        super("Invalid professor!");
    }

    public ProfessorNotFoundException(String message) {
        super(message);
    }
}

