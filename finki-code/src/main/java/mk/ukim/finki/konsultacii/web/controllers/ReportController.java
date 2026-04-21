package mk.ukim.finki.konsultacii.web.controllers;

import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.ConsultationsPerSemester;
import mk.ukim.finki.konsultacii.model.ConsultationsPerWeek;
import mk.ukim.finki.konsultacii.service.ConsultationsPerSemesterService;
import mk.ukim.finki.konsultacii.service.ConsultationsPerWeekService;
import mk.ukim.finki.konsultacii.service.SemesterService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Controller
@AllArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final ConsultationsPerWeekService consultationsPerWeekService;
    private final SemesterService semesterService;
    private final ConsultationsPerSemesterService consultationsPerSemesterService;

    @GetMapping("/weekly")
    public String getReportsPage(Model model,
                                 @RequestParam (required = false) String professorId,
                                 @RequestParam (required = false) Integer consultationsNumber,
                                 @RequestParam (required = false) String semesterCode,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "20") Integer results) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        if (weekStart == null) {
            weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        }

        Page<ConsultationsPerWeek> consultationsPerWeekPage =
                this.consultationsPerWeekService.findAll(pageNum, results, consultationsNumber, professorId, semesterCode,
                        weekStart);

        model.addAttribute("semesters", this.semesterService.findAll());
        model.addAttribute("page", consultationsPerWeekPage);
        model.addAttribute("username", username);
        model.addAttribute("weekStart", weekStart);
        return "reports/weeklyReportsPage";
    }

    @GetMapping("/semester")
    public String getSemesterReportsPage(Model model,
                                 @RequestParam (required = false) String professorId,
                                 @RequestParam (required = false) Integer consultationsNumber,
                                 @RequestParam (required = false) String semesterCode,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "20") Integer results) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Page<ConsultationsPerSemester> consultationsPerSemesterPage =
                this.consultationsPerSemesterService.findAll(pageNum, results, consultationsNumber, professorId, semesterCode);

        model.addAttribute("semesters", this.semesterService.findAll());
        model.addAttribute("page", consultationsPerSemesterPage);
        model.addAttribute("username", username);
        return "reports/semesterReportsPage";
    }
}
