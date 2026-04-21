package mk.ukim.finki.konsultacii.web.controllers;

import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.ConsultationAttendance;
import mk.ukim.finki.konsultacii.model.Professor;
import mk.ukim.finki.konsultacii.model.enumerations.*;
import mk.ukim.finki.konsultacii.model.views.ConsultationView;
import mk.ukim.finki.konsultacii.service.*;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Stream;


@Controller
@RequestMapping("/consultations")
@AllArgsConstructor
public class ConsultationsController {

    private ConsultationAttendanceService consultationAttendanceService;
    private ProfessorService professorService;
    private ConsultationViewService consultationViewService;

    @GetMapping
    public String getConsultationsPage(
            Model model,
            @RequestParam(required = false) ConsultationType type,
            @RequestParam(required = false) String professor,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "3") Integer results) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = isUserAuthenticated(auth);
        boolean isStudent = isStudent();
        String username = isAuthenticated ? auth.getName() : "";

        Page<ConsultationAttendance> attendances = isStudent
                ? consultationAttendanceService.findAttendances(ConsultationTimeFilter.UPCOMING, type, null, "", username, results, pageNum, null, null)
                : Page.empty();

        TreeMap<Character, List<Professor>> professors = professorService.findAllProfessorsSortedByFirstName(professor);

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("username", username);
        model.addAttribute("page", attendances);
        model.addAttribute("professorsMap", professors);
        model.addAttribute("isStudent", isStudent);
        return "index";
    }

    @GetMapping(value = "/professor/{id}")
    public String getConsultationsByProfessor(@PathVariable("id") String professorId,
                                              @RequestParam(defaultValue = "1") Integer regularPageNum,
                                              @RequestParam(defaultValue = "3") Integer regularResults,
                                              @RequestParam(defaultValue = "1") Integer irregularPageNum,
                                              @RequestParam(defaultValue = "3") Integer irregularResults,
                                              Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isStudent()) {
            return "redirect:/consultations/student/professor/" + professorId;
        }

        model.addAttribute("professor", professorService.getProfessorById(professorId));
        model.addAttribute("username", authentication.getName());
        model.addAttribute("regularPage", consultationViewService
                .findConsultations(professorId, ConsultationType.WEEKLY, ConsultationTimeFilter.UPCOMING_TODAY, ConsultationStatus.ACTIVE, regularResults, regularPageNum));
        model.addAttribute("irregularPage", consultationViewService
                .findConsultations(professorId, ConsultationType.ONE_TIME, ConsultationTimeFilter.UPCOMING_TODAY, ConsultationStatus.ACTIVE, irregularResults, irregularPageNum));

        return "consultationsDisplay/professorConsultations";

    }

    @GetMapping(value = "/student/professor/{id}")
    public String getStudentConsultationsByProfessor(@PathVariable("id") String professorId,
                                                     @RequestParam(defaultValue = "1") Integer regularPageNum,
                                                     @RequestParam(defaultValue = "2") Integer regularResults,
                                                     @RequestParam(defaultValue = "1") Integer irregularPageNum,
                                                     @RequestParam(defaultValue = "2") Integer irregularResults,
                                                     Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Page<ConsultationView> regularPage = consultationViewService
                .findConsultations(professorId, ConsultationType.WEEKLY, ConsultationTimeFilter.UPCOMING_TODAY, ConsultationStatus.ACTIVE, regularResults, regularPageNum);
        Page<ConsultationView> irregularPage = consultationViewService
                .findConsultations(professorId, ConsultationType.ONE_TIME, ConsultationTimeFilter.UPCOMING_TODAY, ConsultationStatus.ACTIVE, irregularResults, irregularPageNum);

        List<Long> allIds = Stream.concat(regularPage.stream(), irregularPage.stream())
                .map(ConsultationView::getId)
                .toList();
        Map<Long, ConsultationAttendance> attendanceMap = consultationAttendanceService.getStudentAttendanceMap(allIds, username);

        model.addAttribute("professor", professorService.getProfessorById(professorId));
        model.addAttribute("username", username);
        model.addAttribute("isAuthenticated", !authentication.getPrincipal().equals("anonymousUser"));
        model.addAttribute("regularPage", regularPage);
        model.addAttribute("irregularPage", irregularPage);
        model.addAttribute("consultationAttendanceMap", attendanceMap);

        return "consultationsDisplay/consultations";
    }

    @PostMapping("/schedule")
    public String registerAttendance(@RequestParam("consultationTermId") Long consultationId,
                                       @RequestParam("comment") String comment,
                                       @RequestParam("professorId") String professorId,
                                       RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            consultationAttendanceService.registerStudentForConsultation(consultationId, authentication.getName(), comment);
            redirectAttributes.addFlashAttribute("success", "You have successfully registered for the consultation.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while registering for the consultation.");
        }
        return "redirect:/consultations/student/professor/" + professorId;
    }

    @GetMapping("/schedule")
    public String redirectScheduleGet() {
        return "redirect:/consultations";
    }

    @PostMapping("/cancel")
    @PreAuthorize("@attendanceSecurity.isOwner(#attendanceId, authentication.name)")
    public String cancelAttendance(@RequestParam Long attendanceId,
                                   @RequestParam("professorId") String professorId,
                                   RedirectAttributes redirectAttributes) {
        try {
            consultationAttendanceService.cancelAttendance(attendanceId);
            redirectAttributes.addFlashAttribute("successMessage", "Успешно го откажавте присуството.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Грешка при откажувањето на консултацијата.");
        }

        return "redirect:/consultations/student/professor/" + professorId;
    }

    @GetMapping("/cancel")
    public String redirectCancelGet() {
        return "redirect:/consultations";
    }

    @PostMapping("/add-comment")
    @PreAuthorize("@attendanceSecurity.isOwner(#attendanceId, authentication.name) or @attendanceSecurity.isProfessor(#attendanceId, authentication.name)")
    public String addComment(@RequestParam Long attendanceId,
                             @RequestParam("professorId") String professorId,
                             @RequestParam("newComment") String comment,
                             @RequestParam("consultationId") String consultationId,
                             RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"));
        try {
            if(comment.isBlank()) {
                throw new UnsupportedOperationException("Коментарот не смее да биде празен");
            }

            consultationAttendanceService.addAttendanceComment(attendanceId, authentication.getName(), comment, isStudent);
            redirectAttributes.addFlashAttribute("successMessage", "Успешно додадовте коментар.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Грешка при додавање на коментар.");
        }
        if(!consultationId.isEmpty()){
            return "redirect:/manage-consultations/"+professorId+"/consultation-details/"+consultationId;
        }
        if(professorId.isEmpty()){
            if(isStudent)
                return "redirect:/student-attendances";
            return "redirect:/professor-attendances";
        }

        return "redirect:/consultations/student/professor/" + professorId;
    }

    @GetMapping("/add-comment")
    public String redirectAddCommentGet() {
        return "redirect:/consultations";
    }

    private boolean isStudent(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getPrincipal().equals("anonymousUser")) return false;
        return authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals(AppRole.STUDENT.roleName()));
    }

    private boolean isUserAuthenticated(Authentication auth) {
        return auth != null
               && auth.isAuthenticated()
               && !"anonymousUser".equals(auth.getPrincipal());
    }

}
