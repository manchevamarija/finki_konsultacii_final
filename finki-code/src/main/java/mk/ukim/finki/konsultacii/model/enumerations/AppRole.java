package mk.ukim.finki.konsultacii.model.enumerations;

public enum AppRole {
    PROFESSOR, ADMIN, STUDENT, GUEST;

    public String roleName() {
        return "ROLE_" + this.name();
    }
}
