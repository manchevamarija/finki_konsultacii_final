package mk.ukim.finki.konsultacii.service.implementation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.service.CalendarService;
import mk.ukim.finki.konsultacii.calendar.service.impl.OutlookCalendarServiceImpl;
import mk.ukim.finki.konsultacii.model.Comment;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.ConsultationAttendance;
import mk.ukim.finki.konsultacii.model.Student;
import mk.ukim.finki.konsultacii.model.User;
import mk.ukim.finki.konsultacii.model.dtos.MailSendingStatus;
import mk.ukim.finki.konsultacii.model.dtos.StudentScheduledConsultationDto;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationTimeFilter;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import mk.ukim.finki.konsultacii.model.exceptions.ConsultationTermNotFoundException;
import mk.ukim.finki.konsultacii.model.exceptions.UserNotFoundException;
import mk.ukim.finki.konsultacii.repository.ConsultationAttendanceRepository;
import mk.ukim.finki.konsultacii.repository.ConsultationRepository;
import mk.ukim.finki.konsultacii.repository.StudentRepository;
import mk.ukim.finki.konsultacii.repository.UserRepository;
import mk.ukim.finki.konsultacii.service.ConsultationAttendanceService;
import mk.ukim.finki.konsultacii.service.ConsultationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static mk.ukim.finki.konsultacii.specifications.FieldFilterSpecification.*;

@Service
@AllArgsConstructor
@Slf4j
public class ConsultationAttendanceServiceImpl implements ConsultationAttendanceService {

    private final StudentRepository studentRepository;
    private final ConsultationAttendanceRepository attendsRepository;
    private final ConsultationRepository consultationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ConsultationService consultationService;
    @Qualifier("outlookCalendarService")
    private final OutlookCalendarServiceImpl outlookCalendarService;
    @Qualifier("googleCalendarService")
    private final CalendarService googleCalendarService;

    @Override
    public Page<ConsultationAttendance> findAttendances(ConsultationTimeFilter time, ConsultationType type, ConsultationStatus status, String professorId, String studentIndex,
                                                        int pageSize, int pageNum, Boolean absentStudent, Boolean absentProfessor) {
        Specification<ConsultationAttendance> spec = Specification.where(null);

        if (professorId != null && !professorId.isEmpty()) {
            spec = spec.and(filterEqualsV(ConsultationAttendance.class, "consultation.professor.id", professorId));
        }

        if (studentIndex != null && !studentIndex.isEmpty()) {
            spec = spec.and(filterContainsText(ConsultationAttendance.class, "attendee.index", studentIndex));
        }

        if (type != null) {
            spec = spec.and(filterEqualsV(ConsultationAttendance.class, "consultation.type", type));
        }

        if (status != null) {
            spec = spec.and(filterEqualsV(ConsultationAttendance.class, "consultation.status", status));
        }

        if(absentStudent != null){
            spec = spec.and(filterEqualsV(ConsultationAttendance.class, "reportAbsentStudent", absentStudent));
        }

        if(absentProfessor != null){
            spec = spec.and(filterEqualsV(ConsultationAttendance.class, "reportAbsentProfessor", absentProfessor));
        }

        LocalDateTime now = LocalDateTime.now();
        Sort sort;
        if (time == ConsultationTimeFilter.UPCOMING) {
            spec = spec.and(isUpcoming(ConsultationAttendance.class, "consultation.oneTimeDate", "consultation.startTime", now));
            sort = Sort.by(Sort.Order.asc("consultation.oneTimeDate"), Sort.Order.asc("consultation.startTime"));
        } else if (time == ConsultationTimeFilter.PAST) {
            spec = spec.and(isPast(ConsultationAttendance.class, "consultation.oneTimeDate", "consultation.startTime", now));
            sort = Sort.by(Sort.Order.desc("consultation.oneTimeDate"), Sort.Order.desc("consultation.startTime"));
        } else {
            sort = Sort.by(Sort.Order.asc("consultation.oneTimeDate"), Sort.Order.asc("consultation.startTime"));
        }

        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        return attendsRepository.findAll(spec, pageable);

    }

    @Override
    public List<ConsultationAttendance> getAttendancesByConsultationId(Long consultationId) {
        return attendsRepository.findByConsultationId(consultationId);
    }

    @Override
    public void registerStudentForConsultation(Long consultationId, String index, String comment) {
        Student student = studentRepository.findById(index).orElseThrow(() -> new UserNotFoundException(index));
        Consultation consultation = consultationRepository.findById(consultationId).orElseThrow(() -> new ConsultationTermNotFoundException(consultationId));

        Optional<ConsultationAttendance> existingAttendance = attendsRepository
                .findFirstByConsultation_IdAndAttendee_IndexOrderByIdAsc(consultationId, index);
        if (existingAttendance.isPresent()) {
            log.warn("Student {} already has attendance {} for consultation {}. Skipping duplicate registration.",
                    index, existingAttendance.get().getId(), consultationId);
            return;
        }

        ConsultationAttendance attendance = new ConsultationAttendance(student, consultation);

        if (comment != null && !comment.isBlank()) {
            String studentFullName = String.format("%s %s", student.getName(), student.getLastName());
            Comment c = new Comment(getInitialsForName(studentFullName), false, LocalDateTime.now(), comment);
            attendance.getComments().add(c);
        }

        attendsRepository.save(attendance);
        syncStudentAttendanceToCalendars(consultation, student);

        notificationService.notifyProfessorAboutStudentAttendance(consultation,
                student,
                consultation.getProfessor().getEmail(),
                comment);
    }

    @Override
    public void reportAbsentProfessor(Long attendanceId, Boolean reportAbsentProfessor, String absentProfessorComment) {
        ConsultationAttendance attendance = attendsRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attendance ID"));

        if (reportAbsentProfessor != null && reportAbsentProfessor) {
            attendance.setReportAbsentProfessor(true);
            attendance.setAbsentProfessorComment(absentProfessorComment);
        }

        attendsRepository.save(attendance);
    }

    @Override
    public void reportAbsentStudent(Long attendanceId, Boolean reportAbsentStudent, String absentStudentComment) {
        ConsultationAttendance attendance = attendsRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attendance ID"));

        if (reportAbsentStudent != null && reportAbsentStudent) {
            attendance.setReportAbsentStudent(true);
            attendance.setAbsentStudentComment(absentStudentComment);
        }

        attendsRepository.save(attendance);
    }

    @Override
    public List<CompletableFuture<MailSendingStatus>> cancelAttendance(Long attendanceId) {
        ConsultationAttendance attendance = attendsRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attendance ID"));
        Consultation consultation = consultationService.getConsultationDetails(attendance.getConsultation().getId());
        Student student = attendance.getAttendee();

        removeStudentAttendanceFromCalendars(consultation, student);
        attendsRepository.deleteById(attendanceId);

        return notificationService.notifyProfessorAboutCancelledAttendance(
                consultation,
                student,
                consultation.getProfessor().getEmail()
        );
    }

    @Override
    public List<CompletableFuture<MailSendingStatus>>  addAttendanceComment(Long attendanceId, String username, String comment, boolean isStudent) {
        ConsultationAttendance attendance = attendsRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attendance ID"));
        String fullName;
        Student student;

        User user;
        if(isStudent){
            student = studentRepository.findById(username).orElseThrow(() -> new UserNotFoundException(username));
            fullName = student.getFullName();
        }
        else{
            user = userRepository.findById(username).orElseThrow(() -> new UserNotFoundException(username));
            fullName = user.getName();
        }

        Comment c = new Comment(getInitialsForName(fullName), !isStudent, LocalDateTime.now(), comment);
        attendance.getComments().add(c);
        attendsRepository.save(attendance);

        if(isStudent){
            return notificationService.notifyStudentCommentAdded(
                    attendance,
                    comment
            );
        }
        else{
            return notificationService.notifyProfessorCommentAdded(
                    attendance,
                    comment
            );
        }
    }

    @Override
    public List<CompletableFuture<MailSendingStatus>> addAttendanceCommentAll(Long consultationId, String username,
                                                                              String comment) {
        List<ConsultationAttendance> attendances = attendsRepository.findByConsultationId(consultationId);
        User user = userRepository.findById(username).orElseThrow(() -> new UserNotFoundException(username));

        String fullName = user.getName();
        String initials = getInitialsForName(fullName);
        attendances.forEach(attendance -> {
            attendance.getComments().add(new Comment(initials, true, LocalDateTime.now(), comment));
        });

        attendsRepository.saveAll(attendances)
            .stream()
            .filter(attendance -> attendance.getAttendee().getEmail() != null)
            .forEach(attendance -> {
                notificationService.notifyProfessorCommentAdded(
                    attendance,
                    comment
                );
            });

        return null;
    }

    @Override
    public Map<Long, ConsultationAttendance> getStudentAttendanceMap(List<Long> consultationIds, String studentIndex) {
        if (consultationIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return attendsRepository.findByConsultationIdsAndStudentEmail(consultationIds, studentIndex)
                .stream()
                .collect(Collectors.toMap(
                        ca -> ca.getConsultation().getId(),
                        ca -> ca,
                        (existing, duplicate) -> {
                            ConsultationAttendance kept = existing.getId() <= duplicate.getId() ? existing : duplicate;
                            ConsultationAttendance ignored = kept == existing ? duplicate : existing;
                            log.warn("Duplicate attendances found for consultation {} and student {}. Keeping attendance {} and ignoring {}.",
                                    kept.getConsultation().getId(), studentIndex, kept.getId(), ignored.getId());
                            return kept;
                        },
                        LinkedHashMap::new
                ));
    }

    @Override
    public List<StudentScheduledConsultationDto> findAllFutureActiveConsultationTermsForStudent(String email) {
        Optional<Student> student = this.studentRepository.findByEmail(email);
        return student.map(value -> this.attendsRepository.findAllFutureActiveConsultationsForStudent(value.getIndex())
                .stream().map(ct -> {
                    return new StudentScheduledConsultationDto(ct.getId(), ct.getProfessorName(), ct.getOneTimeDate(),
                            ct.getTimeFrom(), ct.getTimeTo(), ct.getRoom(), ct.getComment());
                }).collect(Collectors.toList())).orElseGet(List::of);

    }


    private String getInitialsForName(String fullName) {
        return Arrays.stream(fullName.trim().split("\\s+"))
            .map(s -> s.substring(0, 1).toUpperCase())
            .collect(Collectors.joining());
    }

    private void syncStudentAttendanceToCalendars(Consultation consultation, Student student) {
        List<String> studentEmails = student.getCalendarEmails();
        if (studentEmails.isEmpty() || consultation.getProfessor() == null) {
            return;
        }

        String professorId = consultation.getProfessor().getId();
        String studentName = student.getFullName();
        String previousOutlookEventId = consultation.getOutlookEventId();
        String outlookEventId = ensureOutlookEventId(consultation, professorId);
        boolean outlookEventRecreated = hasText(outlookEventId)
                && !Objects.equals(previousOutlookEventId, outlookEventId);
        boolean googleCalendarAvailable = hasText(consultation.getGoogleCalendarEventId())
                && googleCalendarService.isConnected(professorId);

        if (outlookEventRecreated) {
            addAllRegisteredStudentsToOutlookEvent(consultation, outlookEventId, professorId);
        }

        studentEmails.forEach(studentEmail -> {
            if (hasText(outlookEventId) && !outlookEventRecreated) {
                outlookCalendarService.addAttendeeToEvent(
                        outlookEventId,
                        studentEmail,
                        studentName,
                        professorId
                );
            }

            if (googleCalendarAvailable) {
                googleCalendarService.addAttendeeToEvent(
                        consultation.getGoogleCalendarEventId(),
                        studentEmail,
                        studentName,
                        professorId
                );
            }
        });
    }

    private void removeStudentAttendanceFromCalendars(Consultation consultation, Student student) {
        List<String> studentEmails = student.getCalendarEmails();
        if (studentEmails.isEmpty() || consultation.getProfessor() == null) {
            return;
        }

        String professorId = consultation.getProfessor().getId();
        String outlookEventId = getExistingOutlookEventId(consultation, professorId);
        boolean googleCalendarAvailable = hasText(consultation.getGoogleCalendarEventId())
                && googleCalendarService.isConnected(professorId);

        studentEmails.forEach(studentEmail -> {
            if (hasText(outlookEventId)) {
                outlookCalendarService.removeAttendeeFromEvent(
                        outlookEventId,
                        studentEmail,
                        professorId
                );
            }

            if (googleCalendarAvailable) {
                googleCalendarService.removeAttendeeFromEvent(
                        consultation.getGoogleCalendarEventId(),
                        studentEmail,
                        professorId
                );
            }
        });
    }

    private void addAllRegisteredStudentsToOutlookEvent(Consultation consultation, String outlookEventId, String professorId) {
        attendsRepository.findByConsultationId(consultation.getId())
                .stream()
                .map(ConsultationAttendance::getAttendee)
                .filter(Objects::nonNull)
                .forEach(attendee -> attendee.getCalendarEmails().forEach(studentEmail ->
                        outlookCalendarService.addAttendeeToEvent(
                                outlookEventId,
                                studentEmail,
                                attendee.getFullName(),
                                professorId
                        )
                ));
    }

    private String ensureOutlookEventId(Consultation consultation, String professorId) {
        if (!outlookCalendarService.isConnected(professorId)) {
            return null;
        }

        String currentEventId = consultation.getOutlookEventId();
        if (hasText(currentEventId) && outlookEventExists(currentEventId, professorId)) {
            return currentEventId;
        }

        if (hasText(currentEventId)) {
            log.warn("Outlook event {} for consultation {} is stale. Recreating event before syncing attendee.",
                    currentEventId, consultation.getId());
        }

        String newEventId = outlookCalendarService.createEvent(consultation, professorId);
        if (hasText(newEventId)) {
            consultation.setOutlookEventId(newEventId);
            consultationRepository.save(consultation);
            return newEventId;
        }

        if (hasText(currentEventId)) {
            consultation.setOutlookEventId(null);
            consultationRepository.save(consultation);
        }
        return null;
    }

    private String getExistingOutlookEventId(Consultation consultation, String professorId) {
        if (!outlookCalendarService.isConnected(professorId)) {
            return null;
        }

        String currentEventId = consultation.getOutlookEventId();
        if (!hasText(currentEventId)) {
            return null;
        }

        if (outlookEventExists(currentEventId, professorId)) {
            return currentEventId;
        }

        log.warn("Outlook event {} for consultation {} is stale. Clearing stored event id.",
                currentEventId, consultation.getId());
        consultation.setOutlookEventId(null);
        consultationRepository.save(consultation);
        return null;
    }

    private boolean outlookEventExists(String eventId, String professorId) {
        return outlookCalendarService.eventExists(eventId, professorId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
