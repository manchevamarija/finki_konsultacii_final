package mk.ukim.finki.konsultacii.model.enumerations;

import lombok.Getter;


public enum UserRole {
    STUDENT(false, true, AppRole.STUDENT),
    // professors
    PROFESSOR(true, false, AppRole.PROFESSOR),
    ACADEMIC_AFFAIR_VICE_DEAN(true, false, AppRole.ADMIN),
    SCIENCE_AND_COOPERATION_VICE_DEAN(true, false, AppRole.ADMIN),
    FINANCES_VICE_DEAN(true, false, AppRole.ADMIN),
    DEAN(true, false, AppRole.ADMIN),
    // staff
    STUDENT_ADMINISTRATION(false, false, AppRole.ADMIN),
    STUDENT_ADMINISTRATION_MANAGER(false, false, AppRole.ADMIN),
    FINANCE_ADMINISTRATION(false, false, AppRole.ADMIN),
    FINANCE_ADMINISTRATION_MANAGER(false, false, AppRole.ADMIN),
    LEGAL_ADMINISTRATION(false, false, AppRole.ADMIN),
    ARCHIVE_ADMINISTRATION(false, false, AppRole.ADMIN),
    ADMINISTRATION_MANAGER(false, false, AppRole.ADMIN),
    // external professors
    EXTERNAL(true, false, AppRole.PROFESSOR);

    private final Boolean professor;

    private final Boolean student;

    @Getter
    public AppRole applicationRole = AppRole.GUEST;

    UserRole(Boolean professor, Boolean student, AppRole role) {
        this.professor = professor;
        this.student = student;
        this.applicationRole = role;
    }

    UserRole(Boolean professor, Boolean student) {
        this.professor = professor;
        this.student = student;
    }

    public Boolean isProfessor() {
        return professor;
    }

    public Boolean isStudent() {
        return student;
    }

    public String roleName() {
        return "ROLE_" + this.name();
    }
}
