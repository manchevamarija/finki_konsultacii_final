package mk.ukim.finki.konsultacii.service;

import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.dtos.IrregularConsultationsFormDto;
import mk.ukim.finki.konsultacii.model.dtos.MailSendingStatus;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public interface IrregularConsultationTermService {

    Consultation create(IrregularConsultationsFormDto dto, String professorEmail);

    List<CompletableFuture<MailSendingStatus>>  edit(Long id, IrregularConsultationsFormDto dto);

}
