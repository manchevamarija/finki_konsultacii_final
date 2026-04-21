package mk.ukim.finki.konsultacii.service.implementation;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.konsultacii.calendar.service.impl.CalendarSyncServiceImpl;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.Professor;
import mk.ukim.finki.konsultacii.model.Room;
import mk.ukim.finki.konsultacii.model.Semester;
import mk.ukim.finki.konsultacii.model.constants.ApplicationConstants;
import mk.ukim.finki.konsultacii.model.dtos.*;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import mk.ukim.finki.konsultacii.model.exceptions.*;
import mk.ukim.finki.konsultacii.repository.ConsultationRepository;
import mk.ukim.finki.konsultacii.repository.ProfessorRepository;
import mk.ukim.finki.konsultacii.service.RegularConsultationTermService;
import mk.ukim.finki.konsultacii.service.RoomService;
import mk.ukim.finki.konsultacii.service.SemesterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RegularConsultationTermServiceImpl implements RegularConsultationTermService {

    private final ConsultationRepository consultationRepository;
    private final ProfessorRepository professorRepository;
    private final RoomService roomService;
    private final SemesterService semesterService;
    private final CalendarSyncServiceImpl calendarSyncService;

    @Transactional
    @Override
    public List<Consultation> create(RegularConsultationFormDto dto, String professorEmail) {
        List<Consultation> regularConsultationTerms = new ArrayList<>();
        Room room = roomService.getByName(dto.getRoomName());
        Professor createdBy = professorRepository.findById(professorEmail)
                .orElseThrow(ProfessorNotFoundException::new);

        Semester semester = semesterService.findByCode(dto.getSemesterCode());

        LocalDate startDate = semester.getEffectiveStartDate();
        LocalDate endDate = semester.getEffectiveEndDate();

        if (startDate.isBefore(LocalDate.now())) {
            startDate = LocalDate.now();
        }

        startDate = getNextOrSame(startDate, dto.getDayOfWeek());

        while (!startDate.isAfter(endDate)) {
            Consultation consultation = consultationRepository.save(
                    new Consultation(createdBy, room, ConsultationType.WEEKLY, startDate,
                            dto.getDayOfWeek(), dto.getStartTime(), dto.getEndTime(),
                            dto.getOnline(), dto.getStudentInstructions(), dto.getMeetingLink())
            );
            calendarSyncService.onCreate(consultation, professorEmail);
            regularConsultationTerms.add(consultation);
            startDate = startDate.plusWeeks(1);
        }

        return regularConsultationTerms;
    }

    private LocalDate getNextOrSame(LocalDate from, DayOfWeek targetDay) {
        LocalDate result = from;
        while (!result.getDayOfWeek().equals(targetDay)) {
            result = result.plusDays(1);
        }
        return result;
    }

    private Map<String, LocalDate> calculateStartDayOfSemester(LocalDate nextOccurrence) {
        Map<String, LocalDate> daysMap = new HashMap<>();
        if (ApplicationConstants.WINTER_SEMESTER_DATE_RANGE.isDateInSemesterRange(nextOccurrence, nextOccurrence.getYear())) {
            daysMap.put("startDay", LocalDate.of(LocalDate.now().getYear(),
                    ApplicationConstants.WINTER_SEMESTER_DATE_RANGE.getStartMonth(),
                    ApplicationConstants.WINTER_SEMESTER_DATE_RANGE.getStartDay()));
            daysMap.put("endDay", LocalDate.of(nextOccurrence.getYear() + 1,
                    ApplicationConstants.WINTER_SEMESTER_DATE_RANGE.getEndMonth(),
                    ApplicationConstants.WINTER_SEMESTER_DATE_RANGE.getEndDay()));
        } else {
            daysMap.put("startDay", LocalDate.of(nextOccurrence.getYear(),
                    ApplicationConstants.SUMMER_SEMESTER_DATE_RANGE.getStartMonth(),
                    ApplicationConstants.SUMMER_SEMESTER_DATE_RANGE.getStartDay()));
            daysMap.put("endDay", LocalDate.of(nextOccurrence.getYear(),
                    ApplicationConstants.SUMMER_SEMESTER_DATE_RANGE.getEndMonth(),
                    ApplicationConstants.SUMMER_SEMESTER_DATE_RANGE.getEndDay()));
        }
        return daysMap;
    }
}
