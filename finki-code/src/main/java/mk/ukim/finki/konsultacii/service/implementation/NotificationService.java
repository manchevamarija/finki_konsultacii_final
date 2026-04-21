package mk.ukim.finki.konsultacii.service.implementation;

import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.ConsultationAttendance;
import mk.ukim.finki.konsultacii.model.Student;
import mk.ukim.finki.konsultacii.model.custom.DayMapper;
import mk.ukim.finki.konsultacii.model.dtos.MailSendingStatus;
import mk.ukim.finki.konsultacii.model.projections.UserAttendanceProjection;
import mk.ukim.finki.konsultacii.repository.ConsultationAttendanceRepository;
import mk.ukim.finki.konsultacii.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final String SUBJECT_CONSULTATION_UPDATED = "Известување за промена на консултации";
    private static final String SUBJECT_CONSULTATION_CANCELED = "Известување за откажани консултации";
    private static final String SUBJECT_STUDENT_ATTENDANCE = "Студент се пријави за консултации";
    private static final String SUBJECT_ATTENDANCE_CANCELLED = "Студент откажа присуство на консултации";
    private static final String SUBJECT_PROFESSOR_COMMENT = "Професорот додаде нов коментар - Консултации";
    private static final String SUBJECT_STUDENT_COMMENT = "Студентот додаде нов коментар - Консултации";

    private static final String TEMPLATE_CONSULTATION_UPDATED = "updated-consultation-template";
    private static final String TEMPLATE_CONSULTATION_CANCELED = "canceled-consultation-template";
    private static final String TEMPLATE_STUDENT_ATTENDANCE = "student-attendance-consultation-template";
    private static final String TEMPLATE_ATTENDANCE_CANCELLED = "student-attendance-cancelled-template";
    private static final String TEMPLATE_COMMENT_ADDED = "comment-added-template";

    private final EmailService emailService;
    private final ConsultationAttendanceRepository consultationRepository;
    private final String baseUrl;

    public NotificationService(EmailService emailService, ConsultationAttendanceRepository consultationRepository,
                               @Value("${app.base-url}") String baseUrl){
        this.emailService = emailService;
        this.baseUrl = baseUrl;
        this.consultationRepository = consultationRepository;
    }

    public List<CompletableFuture<MailSendingStatus>> notifyStudentsAboutUpdatedConsultation(Consultation consultation) {
        return notifyStudents(consultation, SUBJECT_CONSULTATION_UPDATED, TEMPLATE_CONSULTATION_UPDATED);
    }

    public List<CompletableFuture<MailSendingStatus>> notifyStudentsAboutCanceledConsultation(Consultation consultation) {
        return notifyStudents(consultation, SUBJECT_CONSULTATION_CANCELED, TEMPLATE_CONSULTATION_CANCELED);
    }

    public List<CompletableFuture<MailSendingStatus>> notifyProfessorAboutStudentAttendance(
            Consultation consultation, Student student, String professorEmail, String comment) {
        Map<String, Object> model = createConsultationModel(consultation);
        addStudentInfoToModel(model, student);
        model.put("comment", comment);
        model.put("detailsLink", createConsultationDetailsLink(consultation));

        return sendSingleEmail(professorEmail, SUBJECT_STUDENT_ATTENDANCE, TEMPLATE_STUDENT_ATTENDANCE, model);
    }

    public List<CompletableFuture<MailSendingStatus>> notifyProfessorAboutCancelledAttendance(
            Consultation consultation, Student student, String professorEmail) {
        Map<String, Object> model = createConsultationModel(consultation);
        addStudentInfoToModel(model, student);
        model.put("detailsLink", createConsultationDetailsLink(consultation));

        return sendSingleEmail(professorEmail, SUBJECT_ATTENDANCE_CANCELLED, TEMPLATE_ATTENDANCE_CANCELLED, model);
    }

    public List<CompletableFuture<MailSendingStatus>> notifyProfessorCommentAdded(
            ConsultationAttendance attendance, String newComment) {
        Map<String, Object> model = createConsultationModel(attendance.getConsultation());
        model.put("newComment", newComment);
        model.put("name", attendance.getConsultation().getProfessor().getName());

        return sendSingleEmail(attendance.getAttendee().getEmail(), SUBJECT_PROFESSOR_COMMENT, TEMPLATE_COMMENT_ADDED, model);
    }

    public List<CompletableFuture<MailSendingStatus>> notifyStudentCommentAdded(ConsultationAttendance attendance,
                                                                                String newComment) {
        Map<String, Object> model = createConsultationModel(attendance.getConsultation());
        model.put("newComment", newComment);
        model.put("name", attendance.getAttendee().getFullName() + " (" + attendance.getAttendee().getIndex() + ")");
        model.put("studentEmail", attendance.getAttendee().getEmail());

        return sendSingleEmail(attendance.getConsultation().getProfessor().getEmail(), SUBJECT_STUDENT_COMMENT, TEMPLATE_COMMENT_ADDED, model);
    }


    private List<CompletableFuture<MailSendingStatus>> notifyStudents(Consultation consultation, String subject, String template) {
        List<UserAttendanceProjection> attendees = consultationRepository.findAttendeesForConsultation(consultation.getId());
        Map<String, Object> model = createConsultationModel(consultation);

        return attendees.stream()
                .map(attendee -> emailService.sendMail(new String[]{attendee.getEmail()}, subject, template, null, model, null))
                .toList();
    }

    private List<CompletableFuture<MailSendingStatus>> sendSingleEmail(
            String recipientEmail, String subject, String template, Map<String, Object> model) {
        CompletableFuture<MailSendingStatus> mailStatus =
                emailService.sendMail(new String[]{recipientEmail}, subject, template, null, model, null);
        return List.of(mailStatus);
    }

    private Map<String, Object> createConsultationModel(Consultation consultation) {
        Map<String, Object> model = new HashMap<>();
        model.put("professor", consultation.getProfessor().getName());
        model.put("date", consultation.getOneTimeDate().format(DATE_FORMATTER));
        model.put("dayOfWeek", DayMapper.getMacedonianDay(consultation.getOneTimeDate().getDayOfWeek()));
        model.put("startTime", consultation.getStartTime());
        model.put("endTime", consultation.getEndTime());
        model.put("room", consultation.getRoom().getName() + (consultation.getOnline() ? " и Онлајн" : ""));
        model.put("instructions", consultation.getStudentInstructions());
        return model;
    }

    private void addStudentInfoToModel(Map<String, Object> model, Student student) {
        model.put("studentName", student.getFullName());
        model.put("index", student.getIndex());
        model.put("studentEmail", student.getEmail());
    }

    private String createConsultationDetailsLink(Consultation consultation) {
        return String.format("%s/manage-consultations/%s/consultation-details/%s",
                baseUrl, consultation.getProfessor().getId(), consultation.getId());
    }
}
