package mk.ukim.finki.konsultacii.web.controllers;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.konsultacii.calendar.service.CalendarTokenService;
import mk.ukim.finki.konsultacii.calendar.service.GoogleReverseSyncService;
import mk.ukim.finki.konsultacii.calendar.service.OutlookReverseSyncService;
import mk.ukim.finki.konsultacii.model.Consultation;
import mk.ukim.finki.konsultacii.model.ConsultationAttendance;
import mk.ukim.finki.konsultacii.model.Room;
import mk.ukim.finki.konsultacii.model.Semester;
import mk.ukim.finki.konsultacii.model.dtos.*;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationStatus;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationTimeFilter;
import mk.ukim.finki.konsultacii.model.enumerations.ConsultationType;
import mk.ukim.finki.konsultacii.model.exceptions.ConsultationTermNotFoundException;
import mk.ukim.finki.konsultacii.model.views.ConsultationView;
import mk.ukim.finki.konsultacii.repository.ImportRepository;
import mk.ukim.finki.konsultacii.service.*;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.io.IOException;
import java.io.OutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;


@Controller
@RequestMapping("/manage-consultations")
@AllArgsConstructor
@Slf4j
public class ManageConsultationsController {

    private final RoomService roomService;
    private final ConsultationAttendanceService consultationAttendanceService;
    private final RegularConsultationTermService regularConsultationTermService;
    private final IrregularConsultationTermService irregularConsultationTermService;
    private final ConsultationService consultationService;
    private final SemesterService semesterService;
    private final ImportRepository importRepository;
    private final ConsultationViewService consultationViewService;
    private final CalendarTokenService calendarTokenService;
    private final GoogleReverseSyncService googleReverseSyncService;
    private final OutlookReverseSyncService outlookReverseSyncService;

    @GetMapping(value="/{professorId}", params = {"!time", "!status"})
    public String getConsultationPage(Model model,
                                      @ModelAttribute("errorMessage") String errorMessage,
                                      @ModelAttribute("successMessage") String successMessage,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "5") Integer results,
                                      @PathVariable("professorId") String professorId,
                                      RedirectAttributesModelMap redirectModel) {
        redirectModel.put("errorMessage", errorMessage);
        redirectModel.put("successMessage", successMessage);
        redirectModel.put("type", type);
        redirectModel.put("pageNum", pageNum);
        redirectModel.put("results", results);
        redirectModel.put("time", ConsultationTimeFilter.UPCOMING);
        redirectModel.put("status", "");
        return "redirect:/manage-consultations/" + professorId;
    }

    @GetMapping(value="/{professorId}", params = {"time", "status"})
    public String getConsultationPage(Model model,
                                      @ModelAttribute("errorMessage") String errorMessage,
                                      @ModelAttribute("successMessage") String successMessage,
                                      @RequestParam(required = false) ConsultationType type,
                                      @RequestParam ConsultationTimeFilter time,
                                      @RequestParam(required = false) ConsultationStatus status,
                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "5") Integer results,
                                      @PathVariable("professorId") String professorId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        List<DayOfWeek> daysOfWeek = Arrays.stream(DayOfWeek.values()).toList();
        List<Room> rooms = roomService.getAllRooms();

        Page<ConsultationView> consultations = this.consultationViewService.findConsultations(professorId, type, time, status, results, pageNum);
        List<Semester> semesters = semesterService.findAll();

        model.addAttribute("page", consultations);
        model.addAttribute("username", username);
        model.addAttribute("professorId", professorId);
        model.addAttribute("daysOfWeek", daysOfWeek);
        model.addAttribute("rooms", rooms);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("successMessage", successMessage);
        model.addAttribute("minDate", LocalDate.now());
        model.addAttribute("semesters", semesterService.findAll());
        model.addAttribute("currentSemester", semesters.stream()
                .filter(Semester::isCurrent).findFirst()
                .orElse(semesters.isEmpty() ? null : semesters.get(semesters.size() - 1)));
        model.addAttribute("outlookConnected", calendarTokenService.hasOutlookConnection(professorId));
        model.addAttribute("googleConnected", calendarTokenService.hasGoogleConnection(professorId));
        return "manageConsultations/manageConsultations";
    }

    @GetMapping("/{professorId}/create-consultation")
    public String createConsultationGet(@PathVariable("professorId") String professorId,
                                        RedirectAttributes attributes) {
        attributes.addAttribute("errorMessage", "Formata za zakazuvanje se prakja so kopceto Zakazi konsultacii.");
        return "redirect:/manage-consultations/" + professorId;
    }

    @PostMapping("/{professorId}/create-consultation")
    public String createConsultation(@PathVariable("professorId") String professorId,
                                     @RequestParam("isRegular") boolean isRegular,
                                     @RequestParam("startTime") String startTime,
                                     @RequestParam("endTime") String endTime,
                                     @RequestParam("roomName") String roomName,
                                     @RequestParam(value = "online", required = false) Boolean online,
                                     @RequestParam(value = "meetingLink", required = false) String meetingLink,
                                     @RequestParam(value = "studentInstructions", required = false) String studentInstructions,
                                     @RequestParam(value = "dayOfWeek", required = false) String dayOfWeek,
                                     @RequestParam(value = "date", required = false) String date,
                                     @RequestParam(value = "semesterCode", required = false) String semesterCode,
                                     RedirectAttributes attributes) {
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);
        if (start.isAfter(end) || start.equals(end)) {
            attributes.addAttribute("errorMessage", "Времето на започнување мора да биде пред времето на завршување.");
            return "redirect:/manage-consultations/" + professorId;
        }

        if (isRegular) {
            regularConsultationTermService.create(new RegularConsultationFormDto(
                    LocalTime.parse(startTime), LocalTime.parse(endTime), roomName,
                    online != null && online, studentInstructions, meetingLink,
                    DayOfWeek.valueOf(dayOfWeek), semesterCode), professorId);
            attributes.addAttribute("successMessage", "Успешно креирани термини за редовни консултации!");
        } else {
            irregularConsultationTermService.create(new IrregularConsultationsFormDto(
                    LocalTime.parse(startTime), LocalTime.parse(endTime), roomName,
                    online != null && online, studentInstructions, meetingLink,
                    LocalDate.parse(date)), professorId);
            attributes.addAttribute("successMessage", "Успешно креиран термин за дополнителни консултации!");
        }
        return "redirect:/manage-consultations/" + professorId;
    }

    @GetMapping("/{professorId}/edit-consultation/{id}")
    public String editConsultation(@PathVariable("id") Long consultationId,
                                   @PathVariable("professorId") String professorId,
                                   @ModelAttribute("errorMessage") String errorMessage,
                                   @ModelAttribute("successMessage") String successMessage,
                                   Model model) {
        Consultation consultation = consultationService.getConsultationDetails(consultationId);
        if (consultation == null) return "error";

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("daysOfWeek", DayOfWeek.values());
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("username", authentication.getName());
        model.addAttribute("professorId", professorId);
        model.addAttribute("consultation", consultation);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("successMessage", successMessage);
        model.addAttribute("minDate", LocalDate.now());
        return "manageConsultations/editConsultation";
    }

    @PostMapping("/{professorId}/update-consultation/{id}")
    public String updateConsultations(@PathVariable Long id,
                                      @PathVariable("professorId") String professorId,
                                      @RequestParam("editAllUpcoming") boolean editAllUpcoming,
                                      @RequestParam("startTime") String startTime,
                                      @RequestParam("endTime") String endTime,
                                      @RequestParam("roomName") String roomName,
                                      @RequestParam(value = "online", required = false) Boolean online,
                                      @RequestParam(value = "meetingLink", required = false) String meetingLink,
                                      @RequestParam(value = "studentInstructions", required = false) String studentInstructions,
                                      @RequestParam(value = "dayOfWeek", required = false) String dayOfWeek,
                                      @RequestParam(value = "date", required = false) String date,
                                      RedirectAttributes attributes) {
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);
        if (start.isAfter(end) || start.equals(end)) {
            attributes.addAttribute("errorMessage", "Времето на започнување мора да биде пред времето на завршување.");
            return "redirect:/manage-consultations/" + professorId + "/edit-consultation/" + id;
        }

        if (editAllUpcoming) {
            consultationService.editAllUpcomingWeeklyConsultations(id, new RegularConsultationFormDto(
                    LocalTime.parse(startTime), LocalTime.parse(endTime), roomName,
                    online != null && online, studentInstructions, meetingLink, DayOfWeek.valueOf(dayOfWeek)));
            attributes.addFlashAttribute("successMessage", "Успешно ги ажуриравте термините за редовни консултации.");
            return "redirect:/manage-consultations/" + professorId;
        } else {
            irregularConsultationTermService.edit(id, new IrregularConsultationsFormDto(
                    LocalTime.parse(startTime), LocalTime.parse(endTime), roomName,
                    online != null && online, studentInstructions, meetingLink, LocalDate.parse(date)));
            attributes.addFlashAttribute("successMessage", "Успешно го ажуриравте терминот за консултации.");
            return "redirect:/manage-consultations/" + professorId + "/consultation-details/" + id;
        }
    }

    @PostMapping("/{professorId}/delete-all-upcoming/{id}")
    public String deleteWeeklyConsultations(@PathVariable("id") Long consultationId,
                                            @PathVariable("professorId") String professorId,
                                            RedirectAttributes redirectAttributes) {
        try {
            consultationService.deleteAllUpcomingWeeklyConsultations(consultationId);
            redirectAttributes.addFlashAttribute("successMessage", "Сите наредни редовни консултации од терминот се успешно избришани.");
        } catch (ConsultationTermNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Терминот веќе не постои или е автоматски исклучен.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Настана грешка при бришење на терминот.");
        }
        return "redirect:/manage-consultations/" + professorId;
    }

    @PostMapping("/{professorId}/toggle-status/{id}")
    public String toggleStatus(@PathVariable("id") Long consultationId,
                               @PathVariable("professorId") String professorId,
                               RedirectAttributes redirectAttributes) {
        try {
            consultationService.toggleStatus(consultationId);
            redirectAttributes.addFlashAttribute("successMessage", "Успешно го променивте статусот.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manage-consultations/" + professorId;
    }

    @GetMapping("/{professorId}/consultation-details/{id}")
    public String getConsultationDetails(@PathVariable("id") Long consultationId,
                                         @PathVariable("professorId") String professorId,
                                         Model model,
                                         @ModelAttribute("errorMessage") String errorMessage,
                                         @ModelAttribute("successMessage") String successMessage) {
        Consultation consultation = consultationService.getConsultationDetails(consultationId);
        if (consultation == null) return "error";

        List<ConsultationAttendance> attendees = consultationAttendanceService.getAttendancesByConsultationId(consultationId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("successMessage", successMessage);
        model.addAttribute("username", authentication.getName());
        model.addAttribute("consultation", consultation);
        model.addAttribute("attendees", attendees);
        model.addAttribute("attendanceCount", attendees.size());
        return "manageConsultations/consultationDetails";
    }

    @PostMapping("/{professorId}/consultation-details/{id}/add-comment-all")
    public String addCommentAll(@PathVariable("id") Long consultationId,
                                @PathVariable("professorId") String professorId,
                                @RequestParam("newComment") String comment,
                                RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            consultationAttendanceService.addAttendanceCommentAll(consultationId, authentication.getName(), comment);
            redirectAttributes.addFlashAttribute("successMessage", "Успешно додадовте коментар.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Грешка при додавање на коментар.");
        }
        return "redirect:/manage-consultations/" + professorId + "/consultation-details/" + consultationId;
    }

    @GetMapping("/sample-tsv")
    public void sampleTsv(HttpServletResponse response) {
        List<ConsultationDto> example = List.of(
                new ConsultationDto("dimitar.trajanov", "Б 2.2", "15.11.2024", "", "14:30", "15:30", "false", "test1", null, null),
                new ConsultationDto("riste.stojanov", "Амф П", "", "MONDAY", "10:00", "11:00", "true", "", null, null)
        );
        response.setContentType("text/tab-separated-values");
        response.setHeader("Content-Disposition", "attachment; filename=\"consultations_template.tsv\"");
        try (OutputStream outputStream = response.getOutputStream()) {
            importRepository.writeTypeList(ConsultationDto.class, example, outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/import")
    public void importConsultations(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
        List<ConsultationDto> consultations = importRepository.readTypeList(file, ConsultationDto.class);
        List<ConsultationDto> invalidConsultations = consultationService.importConsultations(consultations);
        response.setContentType("text/tab-separated-values");
        response.setHeader("Content-Disposition", "attachment; filename=\"invalid_consultations.tsv\"");
        try (OutputStream outputStream = response.getOutputStream()) {
            importRepository.writeTypeList(ConsultationDto.class, invalidConsultations, outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/{professorId}/delete-one-time/{consultationId}")
    public String deleteOneTimeConsultation(@PathVariable String professorId,
                                            @PathVariable Long consultationId,
                                            RedirectAttributes redirectAttributes) {
        try {
            consultationService.deleteOneTimeConsultation(consultationId);
            redirectAttributes.addFlashAttribute("successMessage", "Дополнителната консултација е успешно избришана.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Настана грешка при бришење на дополнителната консултација.");
        }
        return "redirect:/manage-consultations/" + professorId;
    }
}