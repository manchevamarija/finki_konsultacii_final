package mk.ukim.finki.konsultacii.calendar.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.model.ProfessorCalendarToken;
import mk.ukim.finki.konsultacii.calendar.model.ProfessorCalendarTokenRepository;
import mk.ukim.finki.konsultacii.calendar.model.StudentCalendarToken;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.repository.StudentCalendarTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarTokenServiceImpl implements CalendarTokenService {


    private final ProfessorCalendarTokenRepository tokenRepository;
    private final StudentCalendarTokenRepository studentTokenRepository;
    private final RestTemplate restTemplate;

    @Value("${ms.graph.client-id:}")
    private String msClientId;

    @Value("${ms.graph.client-secret:}")
    private String msClientSecret;

    @Value("${ms.graph.tenant-id:common}")
    private String msTenantId;

    @Value("${google.calendar.client-id:}")
    private String googleClientId;

    @Value("${google.calendar.client-secret:}")
    private String googleClientSecret;

    @Override
    public ProfessorCalendarToken getOrCreate(String professorId) {
        return tokenRepository.findByProfessorId(professorId)
                .orElseGet(() -> tokenRepository.save(new ProfessorCalendarToken(professorId)));
    }

    @Override
    public void saveOutlookTokens(String professorId, String accessToken, String refreshToken, long expiresInSeconds) {
        ProfessorCalendarToken token = getOrCreate(professorId);
        token.setOutlookAccessToken(accessToken);
        token.setOutlookRefreshToken(refreshToken);
        token.setOutlookTokenExpiry(LocalDateTime.now().plusSeconds(expiresInSeconds - 60));
        tokenRepository.save(token);
    }

    @Override
    public String getValidOutlookAccessToken(String professorId) {
        ProfessorCalendarToken token = tokenRepository.findByProfessorId(professorId).orElse(null);
        if (token == null || !token.hasOutlookToken()) return null;

        if (!token.isOutlookTokenExpired()) {
            return token.getOutlookAccessToken();
        }

        try {
            String url = "https://login.microsoftonline.com/" + msTenantId + "/oauth2/v2.0/token";
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", msClientId);
            body.add("client_secret", msClientSecret);
            body.add("refresh_token", token.getOutlookRefreshToken());
            body.add("scope", "Calendars.ReadWrite offline_access");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> resp = response.getBody();
                String newAccess = (String) resp.get("access_token");
                String newRefresh = resp.containsKey("refresh_token") ? (String) resp.get("refresh_token") : token.getOutlookRefreshToken();
                long expiresIn = resp.containsKey("expires_in") ? ((Number) resp.get("expires_in")).longValue() : 3600L;
                saveOutlookTokens(professorId, newAccess, newRefresh, expiresIn);
                return newAccess;
            }
        } catch (Exception e) {
            log.error("Failed to refresh Outlook token for professor {}: {}", professorId, e.getMessage());
        }
        return null;
    }

    @Override
    public void disconnectOutlook(String professorId) {
        tokenRepository.findByProfessorId(professorId).ifPresent(t -> {
            t.setOutlookAccessToken(null);
            t.setOutlookRefreshToken(null);
            t.setOutlookTokenExpiry(null);
            tokenRepository.save(t);
        });
    }

    @Override
    public boolean hasOutlookConnection(String professorId) {
        return tokenRepository.findByProfessorId(professorId)
                .map(ProfessorCalendarToken::hasOutlookToken)
                .orElse(false);
    }

    @Override
    public void saveGoogleTokens(String professorId, String accessToken, String refreshToken, long expiresInSeconds) {
        ProfessorCalendarToken token = getOrCreate(professorId);
        token.setGoogleAccessToken(accessToken);
        token.setGoogleRefreshToken(refreshToken);
        token.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresInSeconds - 60));
        tokenRepository.save(token);
    }

    @Override
    public String getValidGoogleAccessToken(String professorId) {
        ProfessorCalendarToken token = tokenRepository.findByProfessorId(professorId).orElse(null);
        if (token == null || !token.hasGoogleToken()) return null;

        if (!token.isGoogleTokenExpired()) {
            return token.getGoogleAccessToken();
        }

        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", googleClientId);
            body.add("client_secret", googleClientSecret);
            body.add("refresh_token", token.getGoogleRefreshToken());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://oauth2.googleapis.com/token",
                    HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> resp = response.getBody();
                String newAccess = (String) resp.get("access_token");
                long expiresIn = resp.containsKey("expires_in") ? ((Number) resp.get("expires_in")).longValue() : 3600L;
                saveGoogleTokens(professorId, newAccess, token.getGoogleRefreshToken(), expiresIn);
                return newAccess;
            }
        } catch (Exception e) {
            log.error("Failed to refresh Google token for professor {}: {}", professorId, e.getMessage());
        }
        return null;
    }

    @Override
    public void disconnectGoogle(String professorId) {
        tokenRepository.findByProfessorId(professorId).ifPresent(t -> {
            t.setGoogleAccessToken(null);
            t.setGoogleRefreshToken(null);
            t.setGoogleTokenExpiry(null);
            tokenRepository.save(t);
        });
    }

    @Override
    public boolean hasGoogleConnection(String professorId) {
        return tokenRepository.findByProfessorId(professorId)
                .map(ProfessorCalendarToken::hasGoogleToken)
                .orElse(false);
    }

    @Override
    public List<String> findAllProfessorsWithGoogleConnected() {
        return tokenRepository.findAll().stream()
                .filter(ProfessorCalendarToken::hasGoogleToken)
                .map(ProfessorCalendarToken::getProfessorId)
                .toList();
    }

    @Override
    public List<String> findAllProfessorsWithOutlookConnected() {
        return tokenRepository.findAll().stream()
                .filter(ProfessorCalendarToken::hasOutlookToken)
                .map(ProfessorCalendarToken::getProfessorId)
                .toList();
    }

    @Override
    public void saveStudentGoogleTokens(String studentIndex, String accessToken, String refreshToken, long expiresInSeconds) {
        StudentCalendarToken token = studentTokenRepository.findByStudentIndex(studentIndex)
                .orElseGet(() -> new StudentCalendarToken(studentIndex));
        token.setGoogleAccessToken(accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) {
            token.setGoogleRefreshToken(refreshToken);
        }
        token.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresInSeconds - 60));
        studentTokenRepository.save(token);
    }

    @Override
    public String getValidStudentGoogleAccessToken(String studentIndex) {
        StudentCalendarToken token = studentTokenRepository.findByStudentIndex(studentIndex).orElse(null);
        if (token == null || !token.hasGoogleToken()) return null;

        if (!token.isGoogleTokenExpired()) {
            return token.getGoogleAccessToken();
        }

        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", googleClientId);
            body.add("client_secret", googleClientSecret);
            body.add("refresh_token", token.getGoogleRefreshToken());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://oauth2.googleapis.com/token",
                    HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> resp = response.getBody();
                String newAccess = (String) resp.get("access_token");
                String newRefresh = resp.containsKey("refresh_token") ? (String) resp.get("refresh_token") : token.getGoogleRefreshToken();
                long expiresIn = resp.containsKey("expires_in") ? ((Number) resp.get("expires_in")).longValue() : 3600L;
                saveStudentGoogleTokens(studentIndex, newAccess, newRefresh, expiresIn);
                return newAccess;
            }
        } catch (Exception e) {
            log.error("Failed to refresh Google token for student {}: {}", studentIndex, e.getMessage());
        }
        return null;
    }

    @Override
    public void disconnectStudentGoogle(String studentIndex) {
        studentTokenRepository.findByStudentIndex(studentIndex).ifPresent(t -> {
            t.setGoogleAccessToken(null);
            t.setGoogleRefreshToken(null);
            t.setGoogleTokenExpiry(null);
            studentTokenRepository.save(t);
        });
    }

    @Override
    public boolean hasStudentGoogleConnection(String studentIndex) {
        return studentTokenRepository.findByStudentIndex(studentIndex)
                .map(StudentCalendarToken::hasGoogleToken)
                .orElse(false);
    }
}
