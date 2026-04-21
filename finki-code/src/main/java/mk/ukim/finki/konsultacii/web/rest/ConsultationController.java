package mk.ukim.finki.konsultacii.web.rest;

import mk.ukim.finki.konsultacii.model.dtos.ConsultationResponseDto;
import mk.ukim.finki.konsultacii.service.ConsultationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/Consultations/GetTermsByTeacherCode")
    public List<ConsultationResponseDto> getTermsByTeacherCode(
            @RequestParam String teacherCode,
            @RequestParam String token) {
        return consultationService.getConsultationsByProfessor(teacherCode);
    }
}
