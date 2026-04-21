package mk.ukim.finki.konsultacii.service.implementation;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.konsultacii.calendar.service.impl.CalendarSyncServiceImpl;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.Professor;
import mk.ukim.finki.konsultacii.model.Room;
import mk.ukim.finki.konsultacii.model.constants.ApplicationConstants;
import mk.ukim.finki.konsultacii.model.custom.DayMapper;
import mk.ukim.finki.konsultacii.model.dtos.ConsultationDto;
import mk.ukim.finki.konsultacii.model.dtos.ConsultationResponseDto;
import mk.ukim.finki.konsultacii.model.dtos.MailSendingStatus;
import mk.ukim.finki.konsultacii.model.dtos.RegularConsultationFormDto;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import mk.ukim.finki.konsultacii.model.exceptions.ConsultationTermNotFoundException;
import mk.ukim.finki.konsultacii.model.exceptions.IrregularConsultationTermNotFoundException;
import mk.ukim.finki.konsultacii.model.exceptions.RoomNotFoundException;
import mk.ukim.finki.konsultacii.repository.ConsultationAttendanceRepository;
import mk.ukim.finki.konsultacii.repository.ConsultationRepository;
import mk.ukim.finki.konsultacii.repository.ProfessorRepository;
import mk.ukim.finki.konsultacii.repository.RoomRepository;
import mk.ukim.finki.konsultacii.service.ConsultationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationAttendanceRepository consultationAttendanceRepository;
    private final ProfessorRepository professorRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;
    private final CalendarSyncServiceImpl calendarSyncService;

    @Override
    public Consultation getConsultationDetails(Long consultationId) {
        return consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationTermNotFoundException(consultationId));
    }

    @Override
    @Transactional
    public void editAllUpcomingWeeklyConsultations(Long consultationId, RegularConsultationFormDto dto) {
        Consultation startConsultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new IrregularConsultationTermNotFoundException(consultationId));
        Room room = roomRepository.findByName(dto.getRoomName())
                .orElseThrow(() -> new RoomNotFoundException(dto.getRoomName()));

        List<Consultation> matchingConsultations = consultationRepository
                .findAllByProfessor_IdAndTypeAndWeeklyDayOfWeekAndStartTimeAndEndTimeAndOneTimeDateAfter(
                        startConsultation.getProfessor().getId(),
                        ConsultationType.WEEKLY,
                        startConsultation.getWeeklyDayOfWeek(),
                        startConsultation.getStartTime(),
                        startConsultation.getEndTime(),
                        startConsultation.getOneTimeDate().minusDays(1)
                );

        DayOfWeek targetDayOfWeek = dto.getDayOfWeek();
        String professorId = startConsultation.getProfessor().getId();

        List<Consultation> updatedConsultations = new ArrayList<>();
        for (Consultation consultation : matchingConsultations) {
            LocalDate currentDate = consultation.getOneTimeDate();
            LocalDate newOneTimeDate = calculateNextDateForDayOfWeek(currentDate, targetDayOfWeek);

            consultation.setOneTimeDate(newOneTimeDate);
            consultation.setWeeklyDayOfWeek(targetDayOfWeek);
            consultation.setStartTime(dto.getStartTime());
            consultation.setEndTime(dto.getEndTime());
            consultation.setRoom(room);
            consultation.setOnline(dto.getOnline());
            consultation.setStudentInstructions(dto.getStudentInstructions());
            consultation.setMeetingLink(dto.getMeetingLink());

            Consultation saved = consultationRepository.save(consultation);
            calendarSyncService.onUpdate(saved, professorId);
            updatedConsultations.add(saved);
        }
    }

    @Override
    @Transactional
    public void deleteAllUpcomingWeeklyConsultations(Long consultationId) {
        Consultation startConsultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationTermNotFoundException(consultationId));

        List<Consultation> matchingConsultations = consultationRepository
                .findAllByProfessor_IdAndTypeAndWeeklyDayOfWeekAndStartTimeAndEndTimeAndOneTimeDateAfter(
                        startConsultation.getProfessor().getId(),
                        ConsultationType.WEEKLY,
                        startConsultation.getWeeklyDayOfWeek(),
                        startConsultation.getStartTime(),
                        startConsultation.getEndTime(),
                        startConsultation.getOneTimeDate().minusDays(1)
                );

        String professorId = startConsultation.getProfessor().getId();

        for (Consultation consultation : matchingConsultations) {
            long attendanceCount = consultationAttendanceRepository
                    .attendancesCountByConsultationId(consultation.getId());

            if (attendanceCount > 0) {
                String consultationDate = consultation.getOneTimeDate().toString();
                throw new IllegalArgumentException(
                        "Не може да биде избришан терминот на " + consultationDate +
                                " бидејќи има пријавени студенти."
                );
            }
        }

        for (Consultation consultation : matchingConsultations) {
            calendarSyncService.onDelete(consultation, professorId);
        }

        consultationRepository.deleteAll(matchingConsultations);
    }


    @Override
    public List<CompletableFuture<MailSendingStatus>> toggleStatus(Long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new EntityNotFoundException("Consultation with ID " + consultationId + " not found."));

        String professorId = consultation.getProfessor().getId();
        boolean nowActive;

        if (consultation.getStatus() == ConsultationStatus.ACTIVE) {
            consultation.setStatus(ConsultationStatus.INACTIVE);
            consultationRepository.save(consultation);
            nowActive = false;
            calendarSyncService.onToggleStatus(consultation, false, professorId);
            return notificationService.notifyStudentsAboutCanceledConsultation(consultation);
        } else {
            consultation.setStatus(ConsultationStatus.ACTIVE);
            consultationRepository.save(consultation);
            nowActive = true;
            calendarSyncService.onToggleStatus(consultation, true, professorId);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Consultation> listNextWeekConsultationsByProfessor(String id, ConsultationType type) {
        LocalDate today = LocalDate.now();
        LocalDate nextWeekEnd = today.plusDays(6);
        return consultationRepository.findConsultationsForNextWeek(id, type, today, nextWeekEnd);
    }

    @Override
    public List<ConsultationDto> importConsultations(List<ConsultationDto> consultations) {
        return consultations.stream()
                .map(this::saveConsultation)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConsultationResponseDto> getConsultationsByProfessor(String professorId) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(6);
        LocalDate nextTwoWeeks = today.plusWeeks(2);

        return consultationRepository.findNextWeekActiveConsultations(professorId, today, nextWeek, nextTwoWeeks)
                .stream()
                .map(consultation -> {
                    var startDateTime = consultation.getOneTimeDate().atTime(consultation.getStartTime());
                    var endDateTime = consultation.getOneTimeDate().atTime(consultation.getEndTime());
                    String roomName = consultation.getRoom().getName();
                    if (consultation.getOnline() != null && consultation.getOnline()) {
                        roomName += ", Online";
                    }
                    return new ConsultationResponseDto(
                            consultation.getProfessor().getName() + "-" + consultation.getRoom().getName(),
                            "/Date(" + startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + ")/",
                            "/Date(" + endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + ")/",
                            consultation.getCanceledDates().size(),
                            consultation.getId(),
                            consultation.getType().ordinal() + 1,
                            DayMapper.getMacedonianDay(consultation.getDayOfWeek()),
                            consultation.getDayOfWeek().getValue(),
                            "/Date(" + consultation.getOneTimeDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + ")/",
                            consultation.getStartTime().toString(),
                            consultation.getEndTime().toString(),
                            roomName,
                            consultation.getCanceledDates().stream()
                                    .map(date -> "/Date(" + date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + ")/")
                                    .collect(Collectors.toList()),
                            consultation.getCanceledDates().stream()
                                    .map(date -> date.format(dateFormatter))
                                    .collect(Collectors.toList()),
                            0,
                            null,
                            consultation.getOneTimeDate().format(dateFormatter),
                            consultation.getStudentInstructions()
                    );
                })
                .collect(Collectors.toList());
    }


    private Optional<ConsultationDto> saveConsultation(ConsultationDto dto) {
        try {
            Optional<Professor> professorOpt = professorRepository.findById(dto.getProfessorId());
            Optional<Room> roomOpt = roomRepository.findById(dto.getRoomName());

            if (professorOpt.isEmpty()) { dto.setMessage("Invalid professor username."); return Optional.of(dto); }
            if (roomOpt.isEmpty()) { dto.setMessage("Invalid room name."); return Optional.of(dto); }

            if (!dto.getOneTimeDate().isEmpty()) {
                try {
                    String[] parts = dto.getOneTimeDate().split("\\.");
                    LocalDate date = LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
                    Consultation saved = consultationRepository.save(new Consultation(
                            professorOpt.get(), roomOpt.get(), ConsultationType.ONE_TIME, date,
                            date.getDayOfWeek(), LocalTime.parse(dto.getStartTime()), LocalTime.parse(dto.getEndTime()),
                            Boolean.parseBoolean(dto.getOnline()), dto.getStudentInstructions(), dto.getMeetingLink()
                    ));
                    calendarSyncService.onCreate(saved, dto.getProfessorId());
                } catch (Exception e) {
                    dto.setMessage("Invalid date format."); return Optional.of(dto);
                }
            } else {
                LocalDate startDate = calculateStartDayOfSemester().get("startDay");
                LocalDate endDate = calculateStartDayOfSemester().get("endDay");
                startDate = startDate.isAfter(LocalDate.now()) ? startDate : LocalDate.now();
                while (!startDate.isAfter(endDate)) {
                    if (startDate.getDayOfWeek().toString().equals(dto.getWeeklyDayOfWeek())) {
                        Consultation saved = consultationRepository.save(new Consultation(
                                professorOpt.get(), roomOpt.get(), ConsultationType.WEEKLY, startDate,
                                DayOfWeek.valueOf(dto.getWeeklyDayOfWeek()),
                                LocalTime.parse(dto.getStartTime()), LocalTime.parse(dto.getEndTime()),
                                Boolean.parseBoolean(dto.getOnline()), dto.getStudentInstructions(), dto.getMeetingLink()
                        ));
                        calendarSyncService.onCreate(saved, dto.getProfessorId());
                    }
                    startDate = startDate.plusDays(1);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            dto.setMessage("Error saving consultation: " + e.getMessage());
            return Optional.of(dto);
        }
    }

    private Map<String, LocalDate> calculateStartDayOfSemester() {
        Map<String, LocalDate> daysMap = new HashMap<>();
        if (ApplicationConstants.WINTER_SEMESTER_DATE_RANGE.isDateInSemesterRange(LocalDate.now(), LocalDate.now().getYear())) {
            daysMap.put("startDay", LocalDate.of(LocalDate.now().getYear(),
                    ApplicationConstants.WINTER_SEMESTER_DATE_RANGE.getStartMonth(),
                    ApplicationConstants.WINTER_SEMESTER_DATE_RANGE.getStartDay()));
            daysMap.put("endDay", LocalDate.of(LocalDate.now().getYear() + 1,
                    ApplicationConstants.WINTER_SEMESTER_DATE_RANGE.getEndMonth(),
                    ApplicationConstants.WINTER_SEMESTER_DATE_RANGE.getEndDay()));
        } else {
            daysMap.put("startDay", LocalDate.of(LocalDate.now().getYear(),
                    ApplicationConstants.SUMMER_SEMESTER_DATE_RANGE.getStartMonth(),
                    ApplicationConstants.SUMMER_SEMESTER_DATE_RANGE.getStartDay()));
            daysMap.put("endDay", LocalDate.of(LocalDate.now().getYear(),
                    ApplicationConstants.SUMMER_SEMESTER_DATE_RANGE.getEndMonth(),
                    ApplicationConstants.SUMMER_SEMESTER_DATE_RANGE.getEndDay()));
        }
        return daysMap;
    }

    private LocalDate calculateNextDateForDayOfWeek(LocalDate startDate, DayOfWeek targetDayOfWeek) {
        int daysToAdd = targetDayOfWeek.getValue() - startDate.getDayOfWeek().getValue();
        return daysToAdd != 0 ? startDate.plusDays(daysToAdd) : startDate;
    }
    @Override
    @Transactional
    public void deleteOneTimeConsultation(Long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationTermNotFoundException(consultationId));

        if (consultation.getType() != ConsultationType.ONE_TIME) {
            throw new IllegalArgumentException("Овој термин не е дополнителна консултација.");
        }

        long attendanceCount = consultationAttendanceRepository
                .attendancesCountByConsultationId(consultation.getId());

        if (attendanceCount > 0) {
            throw new IllegalArgumentException(
                    "Не може да биде избришан терминот на " + consultation.getOneTimeDate() +
                            " бидејќи има пријавени студенти."
            );
        }

        String professorId = consultation.getProfessor().getId();

        calendarSyncService.onDelete(consultation, professorId);
        consultationRepository.delete(consultation);
    }
}