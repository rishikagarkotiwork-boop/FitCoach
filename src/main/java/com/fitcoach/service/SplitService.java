package com.fitcoach.service;

import com.fitcoach.model.AppUser;
import com.fitcoach.model.PlanDay;
import com.fitcoach.model.SplitDay;
import com.fitcoach.model.WorkoutPlan;
import com.fitcoach.repository.PlanDayRepository;
import com.fitcoach.repository.SplitDayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SplitService {

    /** A weekday together with the user's entry for it (entry may be null = rest day). */
    public record SplitRow(DayOfWeek day, String label, SplitDay entry) {
    }

    private final SplitDayRepository repo;
    private final PlanDayRepository planDayRepo;

    public SplitService(SplitDayRepository repo, PlanDayRepository planDayRepo) {
        this.repo = repo;
        this.planDayRepo = planDayRepo;
    }

    /** Creates or replaces the entry for the given user and weekday. */
    public void save(AppUser user, DayOfWeek day, String focus, String details, String setBy) {
        save(user, day, focus, details, setBy, null);
    }

    private void save(AppUser user, DayOfWeek day, String focus, String details, String setBy, Long sourcePlanId) {
        SplitDay entry = repo.findByUserAndDay(user, day).orElseGet(SplitDay::new);
        entry.setUser(user);
        entry.setDay(day);
        entry.setFocus(focus.trim());
        entry.setDetails(details);
        entry.setSetBy(setBy);
        entry.setSourcePlanId(sourcePlanId);
        repo.save(entry);
    }

    /** Copies every training day of the plan into the user's weekly split. Returns the number of days set. */
    @Transactional
    public int applyPlan(AppUser user, WorkoutPlan plan) {
        List<PlanDay> days = planDayRepo.findByPlanOrderByDayAsc(plan);
        for (PlanDay d : days) {
            save(user, d.getDay(), d.getFocus(), d.getDetails(), "Plan: " + plan.getTitle(), plan.getId());
        }
        return days.size();
    }

    /**
     * Removes the split days that came from the given plan, then refills any freed days
     * from the user's other plans so a same-day entry overwritten earlier comes back.
     */
    @Transactional
    public void removePlan(AppUser user, WorkoutPlan plan, List<WorkoutPlan> remainingPlans) {
        repo.deleteByUserAndSourcePlanId(user, plan.getId());
        repo.flush();
        for (WorkoutPlan other : remainingPlans) {
            for (PlanDay d : planDayRepo.findByPlanOrderByDayAsc(other)) {
                if (repo.findByUserAndDay(user, d.getDay()).isEmpty()) {
                    save(user, d.getDay(), d.getFocus(), d.getDetails(), "Plan: " + other.getTitle(), other.getId());
                }
            }
        }
    }

    public void clear(AppUser user, DayOfWeek day) {
        repo.findByUserAndDay(user, day).ifPresent(repo::delete);
    }

    /** Monday-Sunday rows for one user. */
    public List<SplitRow> week(AppUser user) {
        List<SplitDay> entries = repo.findByUser(user);
        List<SplitRow> rows = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            SplitDay entry = entries.stream().filter(e -> e.getDay() == d).findFirst().orElse(null);
            rows.add(new SplitRow(d, d.getDisplayName(TextStyle.FULL, Locale.ENGLISH), entry));
        }
        return rows;
    }
}
