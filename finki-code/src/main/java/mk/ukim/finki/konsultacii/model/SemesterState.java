package mk.ukim.finki.konsultacii.model;

public enum SemesterState {

    FINISHED(false),
    STARTED(true),
    SCHEDULE_PREPARATION(true),
    TEACHER_SUBJECT_ALLOCATION(true),
    STUDENTS_ENROLLMENT(true),
    DATA_COLLECTION(true),
    INACTIVE(false);

    private final boolean active;

    SemesterState(boolean active) {
        this.active = active;
    }
}
