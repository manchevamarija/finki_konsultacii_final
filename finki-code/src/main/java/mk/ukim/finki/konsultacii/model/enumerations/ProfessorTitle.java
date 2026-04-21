package mk.ukim.finki.konsultacii.model.enumerations;

import lombok.Getter;


@Getter
public enum ProfessorTitle {

    TUTOR(false), TEACHING_ASSISTANT(false),
    ASSISTANT_PROFESSOR(true), ASSOCIATE_PROFESSOR(true),
    PROFESSOR(true), RETIRED(true),
    ELECTED_ASSISTANT_PROFESSOR(true), ELECTED_ASSOCIATE_PROFESSOR(true),
    ELECTED_PROFESSOR(true), EXTERNAL_EXPERT(false);

    private final boolean isProfessor;

    ProfessorTitle(boolean isProfessor) {
        this.isProfessor = isProfessor;
    }
}
