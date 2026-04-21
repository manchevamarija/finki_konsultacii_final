package mk.ukim.finki.konsultacii.calendar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_calendar_token")
@Getter
@Setter
@NoArgsConstructor
public class StudentCalendarToken {

    @Id
    @Column(name = "student_index")
    private String studentIndex;

    @Column(name = "google_access_token", length = 4096)
    private String googleAccessToken;

    @Column(name = "google_refresh_token", length = 4096)
    private String googleRefreshToken;

    @Column(name = "google_token_expiry")
    private LocalDateTime googleTokenExpiry;

    public StudentCalendarToken(String studentIndex) {
        this.studentIndex = studentIndex;
    }

    public boolean hasGoogleToken() {
        return googleRefreshToken != null && !googleRefreshToken.isBlank();
    }

    public boolean isGoogleTokenExpired() {
        return googleTokenExpiry == null || LocalDateTime.now().isAfter(googleTokenExpiry);
    }
}
