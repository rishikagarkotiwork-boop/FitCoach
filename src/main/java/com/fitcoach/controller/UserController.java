package com.fitcoach.controller;

import com.fitcoach.model.*;
import com.fitcoach.repository.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fitcoach.service.SplitService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserController {

    private final AppUserRepository userRepo;
    private final WorkoutPlanRepository planRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final ProgressLogRepository logRepo;
    private final MessageRepository messageRepo;
    private final ContentRepository contentRepo;
    private final FeedbackRepository feedbackRepo;
    private final PasswordEncoder encoder;
    private final SplitService splitService;
    private final PlanDayRepository planDayRepo;

    public UserController(AppUserRepository userRepo, WorkoutPlanRepository planRepo,
                          EnrollmentRepository enrollmentRepo, ProgressLogRepository logRepo,
                          MessageRepository messageRepo, ContentRepository contentRepo,
                          FeedbackRepository feedbackRepo, PasswordEncoder encoder, SplitService splitService,
                          PlanDayRepository planDayRepo) {
        this.splitService = splitService;
        this.planDayRepo = planDayRepo;
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.logRepo = logRepo;
        this.messageRepo = messageRepo;
        this.contentRepo = contentRepo;
        this.feedbackRepo = feedbackRepo;
        this.encoder = encoder;
    }

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        AppUser user = current(auth);
        List<Enrollment> enrollments = enrollmentRepo.findByUser(user);
        Set<Long> activePlanIds = enrollments.stream().map(e -> e.getPlan().getId()).collect(Collectors.toSet());
        List<ProgressLog> logs = logRepo.findByUserOrderByLogDateDescIdDesc(user);

        // oldest first for charts
        List<ProgressLog> chronological = new ArrayList<>(logs);
        chronological.sort(Comparator.comparing(ProgressLog::getLogDate).thenComparing(ProgressLog::getId));

        int totalWorkouts = logs.size();
        int totalMinutes = logs.stream().mapToInt(ProgressLog::getDurationMinutes).sum();
        int totalCalories = logs.stream().map(ProgressLog::getCaloriesBurned).filter(c -> c != null).mapToInt(Integer::intValue).sum();
        LocalDate weekStart = LocalDate.now().minusDays(6);
        long thisWeek = logs.stream().filter(l -> !l.getLogDate().isBefore(weekStart)).count();
        int goal = Math.max(1, user.getWeeklyGoal());

        List<WorkoutPlan> visiblePlans = new ArrayList<>(planRepo.findByAssignedToIsNull());
        List<WorkoutPlan> personal = planRepo.findByAssignedTo(user);
        visiblePlans.addAll(personal);
        model.addAttribute("plans", visiblePlans);
        model.addAttribute("personalPlanIds", personal.stream().map(WorkoutPlan::getId).collect(Collectors.toSet()));
        model.addAttribute("splitRows", splitService.week(user));
        model.addAttribute("activePlanIds", activePlanIds);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("logs", logs);
        model.addAttribute("chartLabels", chronological.stream().map(l -> l.getLogDate().toString()).toList());
        model.addAttribute("chartMinutes", chronological.stream().map(ProgressLog::getDurationMinutes).toList());
        model.addAttribute("chartWeights", chronological.stream().map(ProgressLog::getWeightKg).toList());
        model.addAttribute("totalWorkouts", totalWorkouts);
        model.addAttribute("totalMinutes", totalMinutes);
        model.addAttribute("totalCalories", totalCalories);
        model.addAttribute("thisWeek", thisWeek);
        model.addAttribute("weeklyPct", Math.min(100, (int) (thisWeek * 100 / goal)));
        model.addAttribute("achievements", achievements(totalWorkouts, totalMinutes, thisWeek, goal));
        model.addAttribute("coaches", userRepo.findByRole(Role.COACH));
        model.addAttribute("messages", messageRepo.findBySenderOrRecipientOrderBySentAtDesc(user, user));
        model.addAttribute("myContent", contentRepo.findByAuthor(user));
        return "user/dashboard";
    }

    @GetMapping("/plans/{id}")
    public String planDetails(@PathVariable Long id, Authentication auth, Model model) {
        WorkoutPlan plan = planRepo.findById(id).orElse(null);
        if (plan == null || !canSee(plan, current(auth))) {
            return "redirect:/user";
        }
        model.addAttribute("plan", plan);
        model.addAttribute("planDays", planDayRepo.findByPlanOrderByDayAsc(plan));
        model.addAttribute("enrolled", enrollmentRepo.findByUserAndPlan(current(auth), plan).isPresent());
        return "user/plan";
    }

    @PostMapping("/plans/{id}/enroll")
    public String enroll(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        AppUser user = current(auth);
        WorkoutPlan plan = planRepo.findById(id).orElse(null);
        if (plan == null || !canSee(plan, user)) {
            ra.addFlashAttribute("error", "Plan not found.");
        } else if (enrollmentRepo.findByUserAndPlan(user, plan).isPresent()) {
            ra.addFlashAttribute("error", "You are already following this plan.");
        } else {
            enrollmentRepo.save(new Enrollment(user, plan));
            int days = splitService.applyPlan(user, plan);
            ra.addFlashAttribute("msg", "You are now following '" + plan.getTitle() + "'."
                    + (days > 0 ? " " + days + " training day(s) were added to your weekly split." : ""));
        }
        return "redirect:/user#plans";
    }

    @PostMapping("/plans/{id}/leave")
    public String leave(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        AppUser user = current(auth);
        planRepo.findById(id).flatMap(p -> enrollmentRepo.findByUserAndPlan(user, p)).ifPresent(e -> {
            enrollmentRepo.delete(e);
            splitService.removePlan(user, e.getPlan(), enrollmentRepo.findByUser(user).stream()
                    .map(Enrollment::getPlan).toList());
            ra.addFlashAttribute("msg", "You left '" + e.getPlan().getTitle() + "'; its days were removed from your split.");
        });
        return "redirect:/user#plans";
    }

    @PostMapping("/split")
    public String saveSplitDay(@RequestParam DayOfWeek day, @RequestParam String focus,
                               @RequestParam(required = false) String details,
                               Authentication auth, RedirectAttributes ra) {
        AppUser user = current(auth);
        if (focus.isBlank()) {
            ra.addFlashAttribute("error", "Enter a focus for the day (e.g. Chest & Triceps), or use Clear for a rest day.");
        } else {
            splitService.save(user, day, focus, details, user.getName());
            ra.addFlashAttribute("msg", "Weekly split updated for " + day + ".");
        }
        return "redirect:/user#split";
    }

    @PostMapping("/split/clear")
    public String clearSplitDay(@RequestParam DayOfWeek day, Authentication auth, RedirectAttributes ra) {
        splitService.clear(current(auth), day);
        ra.addFlashAttribute("msg", day + " is now a rest day.");
        return "redirect:/user#split";
    }

    @PostMapping("/progress")
    public String logWorkout(@RequestParam String workoutName, @RequestParam int durationMinutes,
                             @RequestParam(required = false) Long planId,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate logDate,
                             @RequestParam(required = false) Integer caloriesBurned,
                             @RequestParam(required = false) Double weightKg,
                             @RequestParam(required = false) String notes,
                             Authentication auth, RedirectAttributes ra) {
        if (workoutName.isBlank() || durationMinutes < 1) {
            ra.addFlashAttribute("error", "Workout name and a duration of at least 1 minute are required.");
            return "redirect:/user#progress";
        }
        ProgressLog log = new ProgressLog();
        log.setUser(current(auth));
        log.setWorkoutName(workoutName.trim());
        log.setDurationMinutes(durationMinutes);
        log.setLogDate(logDate != null && !logDate.isAfter(LocalDate.now()) ? logDate : LocalDate.now());
        log.setCaloriesBurned(caloriesBurned);
        log.setWeightKg(weightKg);
        log.setNotes(notes);
        if (planId != null) {
            planRepo.findById(planId).ifPresent(log::setPlan);
        }
        logRepo.save(log);
        ra.addFlashAttribute("msg", "Workout logged - progress updated.");
        return "redirect:/user#progress";
    }

    @PostMapping("/messages")
    public String send(@RequestParam Long coachId, @RequestParam String content,
                       Authentication auth, RedirectAttributes ra) {
        AppUser coach = userRepo.findById(coachId).orElse(null);
        if (coach == null || coach.getRole() != Role.COACH || content.isBlank()) {
            ra.addFlashAttribute("error", "Choose a coach and write a message.");
        } else {
            messageRepo.save(new Message(current(auth), coach, content.trim()));
            ra.addFlashAttribute("msg", "Message sent to " + coach.getName() + ".");
        }
        return "redirect:/user#messages";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String name, @RequestParam(required = false) String goal,
                                @RequestParam(required = false) String preferences,
                                @RequestParam int weeklyGoal, @RequestParam(required = false) String newPassword,
                                Authentication auth, RedirectAttributes ra) {
        AppUser user = current(auth);
        if (name.isBlank() || weeklyGoal < 1 || weeklyGoal > 14) {
            ra.addFlashAttribute("error", "Name is required and the weekly goal must be between 1 and 14.");
            return "redirect:/user#profile";
        }
        if (newPassword != null && !newPassword.isEmpty() && newPassword.length() < 6) {
            ra.addFlashAttribute("error", "New password must be at least 6 characters.");
            return "redirect:/user#profile";
        }
        user.setName(name.trim());
        user.setGoal(goal);
        user.setPreferences(preferences);
        user.setWeeklyGoal(weeklyGoal);
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(encoder.encode(newPassword));
        }
        userRepo.save(user);
        ra.addFlashAttribute("msg", "Profile updated successfully.");
        return "redirect:/user#profile";
    }

    @PostMapping("/content")
    public String submitContent(@RequestParam String title, @RequestParam String body,
                                Authentication auth, RedirectAttributes ra) {
        if (title.isBlank() || body.isBlank()) {
            ra.addFlashAttribute("error", "Title and content are required.");
        } else {
            contentRepo.save(new Content(title.trim(), body.trim(), current(auth)));
            ra.addFlashAttribute("msg", "Content submitted for admin approval.");
        }
        return "redirect:/user#share";
    }

    @PostMapping("/feedback")
    public String feedback(@RequestParam String text, @RequestParam int rating,
                           Authentication auth, RedirectAttributes ra) {
        if (text.isBlank() || rating < 1 || rating > 5) {
            ra.addFlashAttribute("error", "Write some feedback and choose a rating from 1 to 5.");
        } else {
            feedbackRepo.save(new Feedback(current(auth), text.trim(), rating));
            ra.addFlashAttribute("msg", "Thanks for your feedback!");
        }
        return "redirect:/user#share";
    }

    private List<String> achievements(int workouts, int minutes, long thisWeek, int goal) {
        List<String> list = new ArrayList<>();
        if (workouts >= 1) list.add("First workout logged");
        if (workouts >= 10) list.add("10 workouts completed");
        if (workouts >= 25) list.add("25 workouts completed");
        if (minutes >= 600) list.add("10 hours of training");
        if (thisWeek >= goal) list.add("Weekly goal reached");
        return list;
    }

    private boolean canSee(WorkoutPlan plan, AppUser user) {
        return plan.getAssignedTo() == null || plan.getAssignedTo().getId().equals(user.getId());
    }

    private AppUser current(Authentication auth) {
        return userRepo.findByEmail(auth.getName()).orElseThrow();
    }
}
