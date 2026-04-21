package mk.ukim.finki.konsultacii.web.controllers;

import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.Professor;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import mk.ukim.finki.konsultacii.service.ConsultationService;
import mk.ukim.finki.konsultacii.service.ProfessorService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.TreeMap;

@RequestMapping(value = {"/", ""})
@Controller
@AllArgsConstructor
public class HomeController {

    private ProfessorService professorService;
    private ConsultationService consultationService;

    @GetMapping
    public String getConsultationsPage(Model model,
                                       @RequestParam(required = false) String professor) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && !authentication.getPrincipal().equals("anonymousUser");
        model.addAttribute("isAuthenticated", isAuthenticated);

        TreeMap<Character, List<Professor>> professors = this.professorService.findAllProfessorsSortedByFirstName(professor);

        model.addAttribute("professorsMap", professors);
        if(isAuthenticated){
            return "redirect:/consultations";
        }
        return "index";
    }

    @GetMapping("/display/{professorId}")
    public String getProfessorConsultations(@PathVariable("professorId") String professorId, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getPrincipal().equals("anonymousUser")){
            Professor professor = this.professorService.getProfessorById(professorId);
            List<Consultation> nextWeekRegularTerms = this.consultationService.listNextWeekConsultationsByProfessor(professorId, ConsultationType.WEEKLY);
            List<Consultation> nextWeekIrregularTerms = this.consultationService.listNextWeekConsultationsByProfessor(professorId, ConsultationType.ONE_TIME);
            model.addAttribute("professor", professor);
            model.addAttribute("regularConsultationTerms", nextWeekRegularTerms);
            model.addAttribute("irregularConsultationTerms", nextWeekIrregularTerms);

            return "consultationsDisplay/publicConsultations";
        }
        if(authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"))){
            return "redirect:/consultations/student/professor/"+professorId;
        }
        return "redirect:/consultations/professor/"+professorId;
    }
}

