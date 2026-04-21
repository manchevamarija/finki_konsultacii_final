package mk.ukim.finki.konsultacii.service;

import mk.ukim.finki.konsultacii.model.ConsultationAttendance;
import mk.ukim.finki.konsultacii.model.dtos.MailSendingStatus;
import mk.ukim.finki.konsultacii.model.dtos.StudentScheduledConsultationDto;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationTimeFilter;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public interface ConsultationAttendanceService {
    Page<ConsultationAttendance> findAttendances(ConsultationTimeFilter time, ConsultationType type, ConsultationStatus status, String professorId, String studentIndex,
                                                 int pageSize, int pageNum, Boolean absentStudent, Boolean absentProfessor);
    List<ConsultationAttendance> getAttendancesByConsultationId(Long consultationId);
    void registerStudentForConsultation(Long consultationId, String index, String comment);
    void reportAbsentProfessor(Long attendanceId, Boolean reportAbsentProfessor, String absentProfessorComment);
    void reportAbsentStudent(Long attendanceId, Boolean reportAbsentStudent, String absentStudentComment);
    List<CompletableFuture<MailSendingStatus>> cancelAttendance(Long attendanceId);
    List<CompletableFuture<MailSendingStatus>> addAttendanceComment(Long attendanceId, String username, String comment, boolean isStudent);
    List<CompletableFuture<MailSendingStatus>> addAttendanceCommentAll(Long consultationId, String username, String comment);
    Map<Long, ConsultationAttendance> getStudentAttendanceMap(List<Long> consultationIds, String studentIndex);
    List<StudentScheduledConsultationDto> findAllFutureActiveConsultationTermsForStudent(String email);

}
