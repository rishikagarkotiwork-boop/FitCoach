package com.fitcoach.config;

import com.fitcoach.model.*;
import com.fitcoach.repository.*;
import com.fitcoach.service.SettingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Creates demo accounts and sample data on first start. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository userRepo;
    private final WorkoutPlanRepository planRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final ProgressLogRepository logRepo;
    private final MessageRepository messageRepo;
    private final ContentRepository contentRepo;
    private final FeedbackRepository feedbackRepo;
    private final SettingService settings;
    private final PasswordEncoder encoder;
    private final com.fitcoach.service.SplitService splitService;
    private final PlanDayRepository planDayRepo;

    public DataSeeder(AppUserRepository userRepo, WorkoutPlanRepository planRepo,
                      EnrollmentRepository enrollmentRepo, ProgressLogRepository logRepo,
                      MessageRepository messageRepo, ContentRepository contentRepo,
                      FeedbackRepository feedbackRepo, SettingService settings, PasswordEncoder encoder,
                      com.fitcoach.service.SplitService splitService, PlanDayRepository planDayRepo) {
        this.splitService = splitService;
        this.planDayRepo = planDayRepo;
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.logRepo = logRepo;
        this.messageRepo = messageRepo;
        this.contentRepo = contentRepo;
        this.feedbackRepo = feedbackRepo;
        this.settings = settings;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) {
            return;
        }

        settings.set(SettingService.SITE_NAME, "FitCoach");
        settings.set(SettingService.ALLOW_REGISTRATION, "true");
        settings.set(SettingService.MAX_PLANS_PER_COACH, "20");
        settings.set(SettingService.ANNOUNCEMENT, "Welcome to FitCoach - train smarter, together!");

        userRepo.save(new AppUser("Site Admin", "admin@fitcoach.com", encoder.encode("admin123"), Role.ADMIN));
        AppUser coach = userRepo.save(new AppUser("Coach Carter", "coach@fitcoach.com", encoder.encode("coach123"), Role.COACH));
        AppUser alice = new AppUser("Alice Johnson", "user@fitcoach.com", encoder.encode("user123"), Role.USER);
        alice.setGoal("Lose 5 kg and build stamina");
        alice.setPreferences("Morning workouts, no gym equipment");
        alice = userRepo.save(alice);
        AppUser bob = new AppUser("Bob Smith", "bob@fitcoach.com", encoder.encode("user123"), Role.USER);
        bob.setGoal("Build muscle");
        bob.setWeeklyGoal(4);
        bob = userRepo.save(bob);

        WorkoutPlan fatBurn = plan(coach, "30-Day Fat Burn", "Beginner-friendly cardio and HIIT plan.", "Beginner", 4,
                "Jumping jacks - 3x30\nBodyweight squats - 3x15\nMountain climbers - 3x20\nPlank - 3x45s",
                "Mon / Wed / Fri - 30 min, Sat - light walk");
        WorkoutPlan strength = plan(coach, "Strength Foundations", "Full-body strength with progressive overload.", "Intermediate", 8,
                "Deadlift - 4x6\nBench press - 4x8\nBarbell row - 4x8\nOverhead press - 3x10",
                "Mon / Tue / Thu / Fri - 45 min");

        enrollmentRepo.save(new Enrollment(alice, fatBurn));
        enrollmentRepo.save(new Enrollment(bob, strength));

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 8; i++) {
            log(alice, fatBurn, "HIIT session " + (i + 1), today.minusDays(i * 3L), 25 + i * 2, 250 + i * 15, 68.0 + i * 0.2);
        }
        for (int i = 0; i < 5; i++) {
            log(bob, strength, "Upper body " + (i + 1), today.minusDays(i * 4L), 45 + i, 320 + i * 10, 80.0 - i * 0.3);
        }

        days(fatBurn, new String[][]{{"MONDAY", "Full body HIIT", "Jumping jacks 3x30, mountain climbers 3x20"},
                {"WEDNESDAY", "Lower body", "Bodyweight squats 3x15, lunges 3x12"},
                {"FRIDAY", "Cardio + core", "30 min jog, plank 3x45s"}});
        days(strength, new String[][]{{"MONDAY", "Push", "Bench press 4x8, overhead press 3x10"},
                {"TUESDAY", "Pull", "Deadlift 4x6, barbell row 4x8"},
                {"THURSDAY", "Legs", "Squat 4x6, lunges 3x12"},
                {"FRIDAY", "Upper body", "Bench press 4x8, rows 4x8"}});
        splitService.applyPlan(alice, fatBurn);
        splitService.applyPlan(bob, strength);

        messageRepo.save(new Message(alice, coach, "Hi coach! Can I swap squats for lunges this week?"));
        messageRepo.save(new Message(coach, alice, "Sure Alice - lunges are a great alternative. Keep your core tight!"));
        messageRepo.save(new Message(bob, coach, "Is 4 sets of bench enough for progress?"));

        contentRepo.save(new Content("5 tips to stay consistent", "Schedule workouts like meetings and track every session.", alice));
        contentRepo.save(new Content("My 3-month transformation", "Sharing my journey with the Strength Foundations plan.", bob));
        Content approved = new Content("Healthy breakfast ideas", "Overnight oats, eggs and fruit are my go-tos.", alice);
        approved.setStatus(ContentStatus.APPROVED);
        contentRepo.save(approved);

        feedbackRepo.save(new Feedback(alice, "Love the plans, very easy to follow!", 5));
        feedbackRepo.save(new Feedback(bob, "Would like more video demonstrations.", 4));
    }

    private void days(WorkoutPlan plan, String[][] rows) {
        for (String[] r : rows) {
            planDayRepo.save(new PlanDay(plan, java.time.DayOfWeek.valueOf(r[0]), r[1], r[2]));
        }
    }

    private WorkoutPlan plan(AppUser coach, String title, String description, String difficulty, int weeks,
                             String exercises, String schedule) {
        WorkoutPlan p = new WorkoutPlan();
        p.setCoach(coach);
        p.setTitle(title);
        p.setDescription(description);
        p.setDifficulty(difficulty);
        p.setDurationWeeks(weeks);
        p.setExercises(exercises);
        p.setSchedule(schedule);
        return planRepo.save(p);
    }

    private void log(AppUser user, WorkoutPlan plan, String name, LocalDate date, int minutes, int calories, double weight) {
        ProgressLog l = new ProgressLog();
        l.setUser(user);
        l.setPlan(plan);
        l.setWorkoutName(name);
        l.setLogDate(date);
        l.setDurationMinutes(minutes);
        l.setCaloriesBurned(calories);
        l.setWeightKg(Math.round(weight * 10) / 10.0);
        logRepo.save(l);
    }
}
