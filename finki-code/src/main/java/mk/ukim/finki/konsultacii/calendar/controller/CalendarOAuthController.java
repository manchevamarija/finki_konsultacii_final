package mk.ukim.finki.konsultacii.calendar.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.calendar.service.impl.StudentGoogleCalendarServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Handles OAuth2 authorization flow for Outlook and Google Calendar.
 *
 * Outlook flow:
 *   1. Professor clicks "Поврзи Outlook" → GET /calendar/outlook/connect
 *   2. Redirect to Microsoft login
 *   3. Microsoft redirects back → GET /calendar/outlook/callback?code=...
 *   4. Exchange code for tokens, save, redirect back to manage-consultations
 *
 * Google flow: identical but different URLs.
 */
@Controller
@RequestMapping("/calendar")
@RequiredArgsConstructor
@Slf4j
public class CalendarOAuthController {

    private final CalendarTokenService tokenService;
    private final StudentGoogleCalendarServiceImpl studentGoogleCalendarService;
    private final RestTemplate restTemplate;


    @Value("${ms.graph.client-id:}")
    private String msClientId;

    @Value("${ms.graph.client-secret:}")
    private String msClientSecret;

    @Value("${ms.graph.tenant-id:common}")
    private String msTenantId;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;


    @Value("${google.calendar.client-id:}")
    private String googleClientId;

    @Value("${google.calendar.client-secret:}")
    private String googleClientSecret;

    // ─── Outlook endpoints ─────────────────────────────────────────────────

    @GetMapping("/outlook/connect")
    public String outlookConnect() {
        if (msClientId.isBlank()) {
            log.warn("Microsoft client ID not configured");
            return "redirect:/manage-consultations/" + currentProfessorId() + "?error=outlook_not_configured";
        }

        String redirectUri = URLEncoder.encode(appBaseUrl + "/calendar/outlook/callback", StandardCharsets.UTF_8);
        String scope = URLEncoder.encode("Calendars.ReadWrite offline_access", StandardCharsets.UTF_8);
        String authUrl = "https://login.microsoftonline.com/" + msTenantId + "/oauth2/v2.0/authorize"
                + "?client_id=" + msClientId
                + "&response_type=code"
                + "&redirect_uri=" + redirectUri
                + "&scope=" + scope
                + "&response_mode=query"
                + "&state=" + currentProfessorId();

        return "redirect:" + authUrl;
    }

    @GetMapping("/outlook/callback")
    public String outlookCallback(@RequestParam(required = false) String code,
                                  @RequestParam(required = false) String state,
                                  @RequestParam(required = false) String error,
                                  RedirectAttributes redirectAttributes) {
        if (error != null || code == null) {
            log.warn("Outlook OAuth error: {}", error);
            redirectAttributes.addFlashAttribute("errorMessage", "Outlook поврзувањето не успеа: " + error);
            return "redirect:/manage-consultations/" + state;
        }

        try {
            String redirectUri = appBaseUrl + "/calendar/outlook/callback";
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", msClientId);
            body.add("client_secret", msClientSecret);
            body.add("code", code);
            body.add("redirect_uri", redirectUri);
            body.add("scope", "Calendars.ReadWrite offline_access");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://login.microsoftonline.com/" + msTenantId + "/oauth2/v2.0/token",
                    HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> resp = response.getBody();
                String accessToken = (String) resp.get("access_token");
                String refreshToken = (String) resp.get("refresh_token");
                long expiresIn = resp.containsKey("expires_in") ? ((Number) resp.get("expires_in")).longValue() : 3600L;

                String professorId = state != null ? state : currentProfessorId();
                tokenService.saveOutlookTokens(professorId, accessToken, refreshToken, expiresIn);
                redirectAttributes.addFlashAttribute("successMessage", "Outlook Calendar е успешно поврзан!");
                return "redirect:/manage-consultations/" + professorId;
            }
        } catch (Exception e) {
            log.error("Outlook token exchange failed: {}", e.getMessage());
        }

        redirectAttributes.addFlashAttribute("errorMessage", "Outlook поврзувањето не успеа. Обидете се повторно.");
        return "redirect:/manage-consultations/" + (state != null ? state : currentProfessorId());
    }

    @GetMapping("/outlook/disconnect")
    public String outlookDisconnect(RedirectAttributes redirectAttributes) {
        String professorId = currentProfessorId();
        tokenService.disconnectOutlook(professorId);
        redirectAttributes.addFlashAttribute("successMessage", "Outlook Calendar е одврзан.");
        return "redirect:/manage-consultations/" + professorId;
    }

    // ─── Google endpoints ──────────────────────────────────────────────────

    @GetMapping("/google/connect")
    public String googleConnect() {
        if (googleClientId.isBlank()) {
            log.warn("Google client ID not configured");
            return "redirect:/manage-consultations/" + currentProfessorId() + "?error=google_not_configured";
        }

        String redirectUri = URLEncoder.encode(appBaseUrl + "/calendar/google/callback", StandardCharsets.UTF_8);
        String scope = URLEncoder.encode("https://www.googleapis.com/auth/calendar.events", StandardCharsets.UTF_8);
        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + googleClientId
                + "&response_type=code"
                + "&redirect_uri=" + redirectUri
                + "&scope=" + scope
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + currentProfessorId();

        return "redirect:" + authUrl;
    }

    @GetMapping("/google/callback")
    public String googleCallback(@RequestParam(required = false) String code,
                                 @RequestParam(required = false) String state,
                                 @RequestParam(required = false) String error,
                                 RedirectAttributes redirectAttributes) {
        if (error != null || code == null) {
            log.warn("Google OAuth error: {}", error);
            redirectAttributes.addFlashAttribute("errorMessage", "Google поврзувањето не успеа: " + error);
            return "redirect:/manage-consultations/" + state;
        }

        try {
            String redirectUri = appBaseUrl + "/calendar/google/callback";
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", googleClientId);
            body.add("client_secret", googleClientSecret);
            body.add("code", code);
            body.add("redirect_uri", redirectUri);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://oauth2.googleapis.com/token",
                    HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> resp = response.getBody();
                String accessToken = (String) resp.get("access_token");
                String refreshToken = (String) resp.get("refresh_token");
                long expiresIn = resp.containsKey("expires_in") ? ((Number) resp.get("expires_in")).longValue() : 3600L;

                String professorId = state != null ? state : currentProfessorId();
                tokenService.saveGoogleTokens(professorId, accessToken, refreshToken, expiresIn);
                redirectAttributes.addFlashAttribute("successMessage", "Google Calendar е успешно поврзан!");
                return "redirect:/manage-consultations/" + professorId;
            }
        } catch (Exception e) {
            log.error("Google token exchange failed: {}", e.getMessage());
        }

        redirectAttributes.addFlashAttribute("errorMessage", "Google поврзувањето не успеа. Обидете се повторно.");
        return "redirect:/manage-consultations/" + (state != null ? state : currentProfessorId());
    }

    @GetMapping("/google/disconnect")
    public String googleDisconnect(RedirectAttributes redirectAttributes) {
        String professorId = currentProfessorId();
        tokenService.disconnectGoogle(professorId);
        redirectAttributes.addFlashAttribute("successMessage", "Google Calendar е исклучен.");
        return "redirect:/manage-consultations/" + professorId;
    }



    @GetMapping("/student/google/connect")
    public String studentGoogleConnect() {
        if (googleClientId.isBlank()) {
            return "redirect:/student-attendances?error=google_not_configured";
        }

        String redirectUri = URLEncoder.encode(appBaseUrl + "/calendar/student/google/callback", StandardCharsets.UTF_8);
        String scope = URLEncoder.encode("https://www.googleapis.com/auth/calendar.events", StandardCharsets.UTF_8);
        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + googleClientId
                + "&response_type=code"
                + "&redirect_uri=" + redirectUri
                + "&scope=" + scope
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=student:" + currentUserId();

        return "redirect:" + authUrl;
    }

    @GetMapping("/student/google/callback")
    public String studentGoogleCallback(@RequestParam(required = false) String code,
                                        @RequestParam(required = false) String state,
                                        @RequestParam(required = false) String error,
                                        RedirectAttributes redirectAttributes) {
        if (error != null || code == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Google поврзувањето не успеа: " + error);
            return "redirect:/student-attendances";
        }

        try {
            String redirectUri = appBaseUrl + "/calendar/student/google/callback";
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", googleClientId);
            body.add("client_secret", googleClientSecret);
            body.add("code", code);
            body.add("redirect_uri", redirectUri);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://oauth2.googleapis.com/token",
                    HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> resp = response.getBody();
                String accessToken = (String) resp.get("access_token");
                String refreshToken = (String) resp.get("refresh_token");
                long expiresIn = resp.containsKey("expires_in") ? ((Number) resp.get("expires_in")).longValue() : 3600L;

                String studentIndex = state != null && state.startsWith("student:")
                        ? state.substring("student:".length())
                        : currentUserId();
                tokenService.saveStudentGoogleTokens(studentIndex, accessToken, refreshToken, expiresIn);
                studentGoogleCalendarService.syncStudentAttendances(studentIndex);
                redirectAttributes.addFlashAttribute("successMessage", "Google Calendar е успешно поврзан за студентскиот профил!");
                return "redirect:/student-attendances";
            }
        } catch (Exception e) {
            log.error("Student Google token exchange failed: {}", e.getMessage(), e);
        }

        redirectAttributes.addFlashAttribute("errorMessage", "Google поврзувањето не успеа. Обидете се повторно.");
        return "redirect:/student-attendances";
    }

    @GetMapping("/student/google/disconnect")
    public String studentGoogleDisconnect(RedirectAttributes redirectAttributes) {
        tokenService.disconnectStudentGoogle(currentUserId());
        redirectAttributes.addFlashAttribute("successMessage", "Google Calendar е исклучен за студентскиот профил.");
        return "redirect:/student-attendances";
    }

    // ─── Helper ───────────────────────────────────────────────────────────

    private String currentProfessorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "";
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "";
    }
}
