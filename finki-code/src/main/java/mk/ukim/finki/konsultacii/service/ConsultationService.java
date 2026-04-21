package mk.ukim.finki.konsultacii.service;

import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.dtos.ConsultationDto;
import mk.ukim.finki.konsultacii.model.dtos.ConsultationResponseDto;
import mk.ukim.finki.konsultacii.model.dtos.MailSendingStatus;
import mk.ukim.finki.konsultacii.model.dtos.RegularConsultationFormDto;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ConsultationService {
    Consultation getConsultationDetails(Long consultationId);
    List<Consultation> listNextWeekConsultationsByProfessor(String id, ConsultationType type);
    List<ConsultationDto> importConsultations(List<ConsultationDto> consultations);
    void editAllUpcomingWeeklyConsultations(Long consultationId, RegularConsultationFormDto dto);
    void deleteAllUpcomingWeeklyConsultations(Long consultationId);
    List<ConsultationResponseDto> getConsultationsByProfessor(String professorId);
    List<CompletableFuture<MailSendingStatus>> toggleStatus(Long consultationId);
    void deleteOneTimeConsultation(Long consultationId);
}
