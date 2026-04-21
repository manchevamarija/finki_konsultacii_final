package mk.ukim.finki.konsultacii.service;

import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.dtos.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public interface RegularConsultationTermService {

    List<Consultation> create(RegularConsultationFormDto dto, String professorEmail);

}
