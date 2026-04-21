package mk.ukim.finki.konsultacii.web.controllers;

import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.ConsultationAttendance;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationTimeFilter;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.service.ConsultationAttendanceService;
import mk.ukim.finki.konsultacii.service.ProfessorService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@AllArgsConstructor
public class ConsultationAttendancesController {
    private final ConsultationAttendanceService consultationAttendanceService;
    private final ProfessorService professorService;
    private final CalendarTokenService calendarTokenService;

    @GetMapping(value="/student-attendances", params = "!time")
    public String getStudentAttendances(RedirectAttributesModelMap redirectModel) {
        redirectModel.put("time", ConsultationTimeFilter.UPCOMING);
        return "redirect:/student-attendances";
    }

    @GetMapping(value = "/student-attendances", params = "time")
    public String getStudentAttendances(Model model,
                                        @RequestParam(required = false) ConsultationType type,
                                        @RequestParam(required = true) ConsultationTimeFilter time,
                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer results) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String studentUsername = authentication.getName();

        Page<ConsultationAttendance> attendancesPage = consultationAttendanceService.
                findAttendances(time, type, null, "", studentUsername, results, pageNum, null, null);

        model.addAttribute("page", attendancesPage);
        model.addAttribute("username", studentUsername);
        model.addAttribute("googleCalendarConnected", calendarTokenService.hasStudentGoogleConnection(studentUsername));
        return "bookedConsultations/studentBookedConsultations";
    }

    @PostMapping("/student-attendances/report-absent-professor")
    @PreAuthorize("@attendanceSecurity.isOwner(#attendanceId, authentication.name)")
    public String reportAbsentProfessor(@RequestParam Long attendanceId,
                                        @RequestParam(required = false) Boolean reportAbsentProfessor,
                                        @RequestParam(required = false) String absentProfessorComment) {
        consultationAttendanceService.reportAbsentProfessor(attendanceId, reportAbsentProfessor, absentProfessorComment);
        return "redirect:/student-attendances";
    }

    @PostMapping("/student-attendances/cancel")
    @PreAuthorize("@attendanceSecurity.isOwner(#attendanceId, authentication.name)")
    public String cancelAttendance(@RequestParam Long attendanceId,
                                   RedirectAttributes redirectAttributes) {
        try {
            consultationAttendanceService.cancelAttendance(attendanceId);
            redirectAttributes.addFlashAttribute("successMessage", "Успешно го откажавте присуството.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Грешка при откажувањето на консултацијата.");
        }

        return "redirect:/student-attendances";
    }


    @GetMapping(value="/professor-attendances", params = {"!time", "!status"})
    public String getProfessorAttendances(RedirectAttributesModelMap redirectModel) {
        redirectModel.put("time", ConsultationTimeFilter.UPCOMING);
        redirectModel.put("status", ConsultationStatus.ACTIVE);
        return "redirect:/professor-attendances";
    }

    @GetMapping(value = "/professor-attendances", params = {"time", "status"})
    public String getProfessorAttendances(Model model,
                                          @RequestParam(required = false) ConsultationType type,
                                          @RequestParam(required = true) ConsultationTimeFilter time,
                                          @RequestParam(required = true) ConsultationStatus status,
                                          @RequestParam(required = false) String studentIndex,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "5") Integer results) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String professorUsername = authentication.getName();

        Page<ConsultationAttendance> attendancePage = consultationAttendanceService.findAttendances(time, type,
                status, professorUsername, studentIndex, results, pageNum, null, null);

        Map<Consultation, List<ConsultationAttendance>> groupedByConsultation =
                attendancePage.getContent().stream()
                        .collect(Collectors.groupingBy(ConsultationAttendance::getConsultation));

        model.addAttribute("groupedAttendances", groupedByConsultation);
        model.addAttribute("page", attendancePage);
        model.addAttribute("professorUsername", professorUsername);
        model.addAttribute("username", professorUsername);
        return "bookedConsultations/professorBookedConsultations";
    }

    @PostMapping("/professor-attendances/add-comment-all")
    @PreAuthorize("@consultationSecurity.isOwner(#consultationId, authentication.name)")
    public String addCommentAll(@RequestParam("newComment") String comment,
                                @RequestParam("consultationId") Long consultationId,
                                RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            if(comment.isBlank()) {
                throw new UnsupportedOperationException("Коментарот не смее да биде празен");
            }

            consultationAttendanceService.addAttendanceCommentAll(consultationId, authentication.getName(), comment);
            redirectAttributes.addFlashAttribute("successMessage", "Успешно додадовте коментар.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Грешка при додавање на коментар.");
        }

        return "redirect:/professor-attendances";
    }

    @PostMapping("/professor-attendances/report-absent-student")
    @PreAuthorize("@attendanceSecurity.isProfessor(#attendanceId, authentication.name)")
    public String reportAbsentStudent(@RequestParam Long attendanceId,
                                      @RequestParam(required = false) Boolean reportAbsentStudent,
                                      @RequestParam(required = false) String absentStudentComment) {
        consultationAttendanceService.reportAbsentStudent(attendanceId, reportAbsentStudent, absentStudentComment);
        return "redirect:/professor-attendances";
    }

    @GetMapping(value="/admin-attendances", params = "!time")
    public String getAdminAttendances(RedirectAttributesModelMap redirectModel) {
        redirectModel.put("time", ConsultationTimeFilter.UPCOMING);
        return "redirect:/admin-attendances";
    }

    @GetMapping(value = "/admin-attendances", params = "time")
    public String getAdminAttendances(Model model,
                                      @RequestParam(required = false) ConsultationType type,
                                      @RequestParam(required = true) ConsultationTimeFilter time,
                                      @RequestParam(required = false) String professorId,
                                      @RequestParam(required = false) String studentIndex,
                                      @RequestParam(required = false) Boolean absentStudent,
                                      @RequestParam(required = false) Boolean absentProfessor,
                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer results) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Page<ConsultationAttendance> attendancePage = consultationAttendanceService.findAttendances(time, type,
                null, professorId, studentIndex, results, pageNum, absentStudent, absentProfessor);

        model.addAttribute("username", username);
        model.addAttribute("page", attendancePage);
        model.addAttribute("professors", professorService.listAllProfessors(""));
        return "bookedConsultations/adminBookedConsultations";
    }
}
