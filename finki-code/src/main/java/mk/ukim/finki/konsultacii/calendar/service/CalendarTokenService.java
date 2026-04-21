package mk.ukim.finki.konsultacii.calendar.service;

import mk.ukim.finki.konsultacii.calendar.model.ProfessorCalendarToken;

import java.util.List;

public interface CalendarTokenService {

    void saveOutlookTokens(String professorId, String accessToken, String refreshToken, long expiresInSeconds);
    String getValidOutlookAccessToken(String professorId);
    void disconnectOutlook(String professorId);

    void saveGoogleTokens(String professorId, String accessToken, String refreshToken, long expiresInSeconds);
    String getValidGoogleAccessToken(String professorId);
    void disconnectGoogle(String professorId);

    ProfessorCalendarToken getOrCreate(String professorId);
    boolean hasOutlookConnection(String professorId);
    boolean hasGoogleConnection(String professorId);


    List<String> findAllProfessorsWithGoogleConnected();
    List<String> findAllProfessorsWithOutlookConnected();

    void saveStudentGoogleTokens(String studentIndex, String accessToken, String refreshToken, long expiresInSeconds);
    String getValidStudentGoogleAccessToken(String studentIndex);
    void disconnectStudentGoogle(String studentIndex);
    boolean hasStudentGoogleConnection(String studentIndex);
}
