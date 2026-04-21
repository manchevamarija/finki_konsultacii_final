package mk.ukim.finki.konsultacii.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {
    SseEmitter subscribe();
    void sendConsultationUpdate();
}