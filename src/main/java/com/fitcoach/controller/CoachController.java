package com.fitcoach.controller;

import com.fitcoach.model.*;
import com.fitcoach.repository.*;
import com.fitcoach.service.CleanupService;
import com.fitcoach.service.SettingService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fitcoach.service.SplitService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/coach")
public class CoachController {

    public record ClientRow(Long id, String name, String goal, int workouts, int totalMinutes,
                            Double latestWeight, LocalDate lastActive) {
    }

    public record SplitView(String name, Long id, List<SplitService.SplitRow> rows) {
    }

    private final SplitService splitService;
    private final PlanDayRepository planDayRepo;
    private final AppUserRepository userRepo;
    private final WorkoutPlanRepository planRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final ProgressLogRepository logRepo;
    private final MessageRepository messageRepo;
    private final CleanupService cleanup;
    private final SettingService settings;

    public CoachController(AppUserRepository userRepo, WorkoutPlanRepository planRepo,
                           EnrollmentRepository enrollmentRepo, ProgressLogRepository logRepo,
                           MessageRepository messageRepo, CleanupService cleanup, SettingService settings,
                           SplitService splitService, PlanDayRepository planDayRepo) {
        this.splitService = splitService;
        this.planDayRepo = planDayRepo;
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.logRepo = logRepo;
        this.messageRepo = messageRepo;
        this.cleanup = cleanup;
        this.settings = settings;
    }

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        AppUser coach = current(auth);
        List<WorkoutPlan> plans = planRepo.findByCoach(coach);
        List<Enrollment> enrollments = enrollmentRepo.findByPlanCoach(coach);

        List<AppUser> clients = List.copyOf(enrollments.stream()
                .collect(Collectors.toMap(e -> e.getUser().getId(), Enrollment::getUser, (a, b) -> a, LinkedHashMap::new))
                .values());

        List<ClientRow> rows = clients.stream().map(c -> {
            List<ProgressLog> logs = logRepo.findByUserOrderByLogDateDescIdDesc(c);
            Double weight = logs.stream().map(ProgressLog::getWeightKg).filter(w -> w != null).findFirst().orElse(null);
            LocalDate last = logs.isEmpty() ? null : logs.get(0).getLogDate();
            int minutes = logs.stream().mapToInt(ProgressLog::getDurationMinutes).sum();
            return new ClientRow(c.getId(), c.getName(), c.getGoal(), logs.size(), minutes, weight, last);
        }).toList();

        List<Message> history = messageRepo.findBySenderOrRecipientOrderBySentAtDesc(coach, coach);
        List<Message> recent = history.stream()
                .filter(m -> m.getRecipient().getId().equals(coach.getId())).limit(5).toList();

        model.addAttribute("plans", plans);
        model.addAttribute("weekdays", DayOfWeek.values());
        model.addAttribute("clients", rows);
        List<AppUser> allUsers = userRepo.findByRole(Role.USER);
        model.addAttribute("recipients", allUsers);
        model.addAttribute("splitUsers", allUsers.stream().map(u -> new SplitView(u.getName(), u.getId(), splitService.week(u))).toList());
        model.addAttribute("recent", recent);
        model.addAttribute("history", history);

        // progress chart: total minutes per client
        model.addAttribute("progressLabels", rows.stream().map(ClientRow::name).toList());
        model.addAttribute("progressData", rows.stream().map(ClientRow::totalMinutes).toList());

        // plan analytics: enrolments and logged workouts per plan
        model.addAttribute("planLabels", plans.stream().map(WorkoutPlan::getTitle).toList());
        model.addAttribute("planEnrolled", plans.stream().map(enrollmentRepo::countByPlan).toList());
        model.addAttribute("planLogs", plans.stream().map(logRepo::countByPlan).toList());
        return "coach/dashboard";
    }

    @PostMapping("/plans")
    public String createPlan(@RequestParam String title, @RequestParam(required = false) String description,
                             @RequestParam String difficulty, @RequestParam int durationWeeks,
                             @RequestParam(required = false) Long assignedToId,
                             @RequestParam Map<String, String> form,
                             Authentication auth, RedirectAttributes ra) {
        AppUser coach = current(auth);
        AppUser assignee = assignedToId == null ? null : userRepo.findById(assignedToId).orElse(null);

        // one training day per weekday that has a focus filled in (form fields focus_MONDAY / details_MONDAY ...)
        List<PlanDay> days = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            String focus = form.getOrDefault("focus_" + d.name(), "").trim();
            if (!focus.isEmpty()) {
                days.add(new PlanDay(null, d, focus, form.getOrDefault("details_" + d.name(), "").trim()));
            }
        }

        if (assignedToId != null && (assignee == null || assignee.getRole() != Role.USER)) {
            ra.addFlashAttribute("error", "Choose a valid user for the personalised plan.");
        } else if (title.isBlank() || days.isEmpty() || durationWeeks < 1) {
            ra.addFlashAttribute("error", "Title, a duration of at least 1 week and at least one training day are required.");
        } else if (planRepo.findByCoach(coach).size() >= settings.getInt(SettingService.MAX_PLANS_PER_COACH, 20)) {
            ra.addFlashAttribute("error", "You have reached the maximum number of plans allowed by the system.");
        } else {
            WorkoutPlan plan = new WorkoutPlan();
            plan.setCoach(coach);
            plan.setTitle(title.trim());
            plan.setDescription(description);
            plan.setDifficulty(difficulty);
            plan.setDurationWeeks(durationWeeks);
            plan.setExercises(days.stream()
                    .flatMap(d -> d.getDetails().isEmpty() ? java.util.stream.Stream.of(d.getFocus())
                            : java.util.stream.Stream.of(d.getDetails().split("\\R|,")))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining("\n")));
            plan.setSchedule(days.stream()
                    .map(d -> d.getDay().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + ": " + d.getFocus())
                    .collect(Collectors.joining(", ")));
            plan.setAssignedTo(assignee);
            planRepo.save(plan);
            for (PlanDay d : days) {
                d.setPlan(plan);
                planDayRepo.save(d);
            }
            if (assignee != null) {
                enrollmentRepo.save(new Enrollment(assignee, plan));
                splitService.applyPlan(assignee, plan);
                messageRepo.save(new Message(coach, assignee,
                        "I've created a personalised plan for you: '" + plan.getTitle() + "'. Find it under your active plans."));
                ra.addFlashAttribute("msg", "Personalised plan '" + plan.getTitle() + "' created and assigned to " + assignee.getName() + ".");
            } else {
                ra.addFlashAttribute("msg", "Workout plan '" + plan.getTitle() + "' created successfully.");
            }
        }
        return "redirect:/coach#plans";
    }

    @PostMapping("/plans/{id}/delete")
    public String deletePlan(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        WorkoutPlan plan = planRepo.findById(id).orElse(null);
        if (plan == null || !plan.getCoach().getId().equals(current(auth).getId())) {
            ra.addFlashAttribute("error", "Plan not found.");
        } else {
            cleanup.deletePlan(plan);
            ra.addFlashAttribute("msg", "Workout plan '" + plan.getTitle() + "' deleted.");
        }
        return "redirect:/coach#plans";
    }

    @PostMapping("/split")
    public String setSplit(@RequestParam Long userId, @RequestParam DayOfWeek day, @RequestParam String focus,
                           @RequestParam(required = false) String details,
                           Authentication auth, RedirectAttributes ra) {
        AppUser user = userRepo.findById(userId).orElse(null);
        if (user == null || user.getRole() != Role.USER || focus.isBlank()) {
            ra.addFlashAttribute("error", "Choose a user, a day and a focus for the split.");
        } else {
            splitService.save(user, day, focus, details, current(auth).getName());
            ra.addFlashAttribute("msg", day + " updated in " + user.getName() + "'s weekly split.");
        }
        return "redirect:/coach#split";
    }

    @PostMapping("/split/clear")
    public String clearSplit(@RequestParam Long userId, @RequestParam DayOfWeek day, RedirectAttributes ra) {
        userRepo.findById(userId).ifPresent(u -> splitService.clear(u, day));
        ra.addFlashAttribute("msg", "Split day cleared.");
        return "redirect:/coach#split";
    }

    @PostMapping("/messages")
    public String send(@RequestParam Long recipientId, @RequestParam String content,
                       Authentication auth, RedirectAttributes ra) {
        AppUser recipient = userRepo.findById(recipientId).orElse(null);
        if (recipient == null || recipient.getRole() != Role.USER || content.isBlank()) {
            ra.addFlashAttribute("error", "Choose a user and write a message.");
        } else {
            messageRepo.save(new Message(current(auth), recipient, content.trim()));
            ra.addFlashAttribute("msg", "Message sent to " + recipient.getName() + ".");
        }
        return "redirect:/coach#messages";
    }

    private AppUser current(Authentication auth) {
        return userRepo.findByEmail(auth.getName()).orElseThrow();
    }
}
