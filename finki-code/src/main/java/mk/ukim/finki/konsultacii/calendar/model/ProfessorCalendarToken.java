package mk.ukim.finki.konsultacii.calendar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "professor_calendar_token")
@Getter
@Setter
@NoArgsConstructor
public class ProfessorCalendarToken {

    @Id
    @Column(name = "professor_id")
    private String professorId;

    @Column(name = "outlook_access_token", length = 4096)
    private String outlookAccessToken;

    @Column(name = "outlook_refresh_token", length = 4096)
    private String outlookRefreshToken;

    @Column(name = "outlook_token_expiry")
    private LocalDateTime outlookTokenExpiry;

    @Column(name = "google_access_token", length = 4096)
    private String googleAccessToken;

    @Column(name = "google_refresh_token", length = 4096)
    private String googleRefreshToken;

    @Column(name = "google_token_expiry")
    private LocalDateTime googleTokenExpiry;

    public ProfessorCalendarToken(String professorId) {
        this.professorId = professorId;
    }

    public boolean hasOutlookToken() {
        return outlookRefreshToken != null && !outlookRefreshToken.isBlank();
    }

    public boolean hasGoogleToken() {
        return googleRefreshToken != null && !googleRefreshToken.isBlank();
    }

    public boolean isOutlookTokenExpired() {
        return outlookTokenExpiry == null || LocalDateTime.now().isAfter(outlookTokenExpiry);
    }

    public boolean isGoogleTokenExpired() {
        return googleTokenExpiry == null || LocalDateTime.now().isAfter(googleTokenExpiry);
    }
}
