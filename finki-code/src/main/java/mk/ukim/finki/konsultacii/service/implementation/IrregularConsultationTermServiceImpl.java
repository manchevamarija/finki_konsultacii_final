package mk.ukim.finki.konsultacii.service.implementation;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.konsultacii.calendar.service.impl.CalendarSyncServiceImpl;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.Professor;
import mk.ukim.finki.konsultacii.model.Room;
import mk.ukim.finki.konsultacii.model.dtos.IrregularConsultationsFormDto;
import mk.ukim.finki.konsultacii.model.dtos.MailSendingStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import mk.ukim.finki.konsultacii.model.exceptions.IrregularConsultationTermNotFoundException;
import mk.ukim.finki.konsultacii.model.exceptions.ProfessorNotFoundException;
import mk.ukim.finki.konsultacii.repository.ConsultationRepository;
import mk.ukim.finki.konsultacii.repository.ProfessorRepository;
import mk.ukim.finki.konsultacii.service.IrregularConsultationTermService;
import mk.ukim.finki.konsultacii.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class IrregularConsultationTermServiceImpl implements IrregularConsultationTermService {

    private final ConsultationRepository consultationRepository;
    private final ProfessorRepository professorRepository;
    private final RoomService roomService;
    private final NotificationService notificationService;
    private final CalendarSyncServiceImpl calendarSyncService;

    @Override
    public Consultation create(IrregularConsultationsFormDto dto, String professorEmail) {
        Room room = roomService.getByName(dto.getRoomName());
        Professor professor = professorRepository.findById(professorEmail)
                .orElseThrow(ProfessorNotFoundException::new);

        Consultation consultation = consultationRepository.save(
                new Consultation(professor, room, ConsultationType.ONE_TIME,
                        dto.getDate(), dto.getDate().getDayOfWeek(),
                        dto.getStartTime(), dto.getEndTime(),
                        dto.getOnline(), dto.getStudentInstructions(), dto.getMeetingLink())
        );

        // Sync to calendars
        calendarSyncService.onCreate(consultation, professorEmail);

        return consultation;
    }

    @Override
    @Transactional
    public List<CompletableFuture<MailSendingStatus>> edit(Long id, IrregularConsultationsFormDto dto) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new IrregularConsultationTermNotFoundException(id));
        Room room = roomService.getByName(dto.getRoomName());

        consultation.setOneTimeDate(dto.getDate());
        consultation.setWeeklyDayOfWeek(dto.getDate().getDayOfWeek());
        consultation.setStartTime(dto.getStartTime());
        consultation.setEndTime(dto.getEndTime());
        consultation.setRoom(room);
        consultation.setOnline(dto.getOnline());
        consultation.setStudentInstructions(dto.getStudentInstructions());
        consultation.setMeetingLink(dto.getMeetingLink());

        Consultation updatedConsultation = consultationRepository.save(consultation);

        String professorId = updatedConsultation.getProfessor().getId();
        calendarSyncService.onUpdate(updatedConsultation, professorId);

        return notificationService.notifyStudentsAboutUpdatedConsultation(updatedConsultation);
    }
}
