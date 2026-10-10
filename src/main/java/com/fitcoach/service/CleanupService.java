package com.fitcoach.service;

import com.fitcoach.model.AppUser;
import com.fitcoach.model.ProgressLog;
import com.fitcoach.model.WorkoutPlan;
import com.fitcoach.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Removes users and plans together with the rows that reference them. */
@Service
public class CleanupService {

    private final AppUserRepository userRepo;
    private final WorkoutPlanRepository planRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final ProgressLogRepository logRepo;
    private final MessageRepository messageRepo;
    private final ContentRepository contentRepo;
    private final FeedbackRepository feedbackRepo;
    private final SplitDayRepository splitRepo;
    private final PlanDayRepository planDayRepo;

    public CleanupService(AppUserRepository userRepo, WorkoutPlanRepository planRepo,
                          EnrollmentRepository enrollmentRepo, ProgressLogRepository logRepo,
                          MessageRepository messageRepo, ContentRepository contentRepo,
                          FeedbackRepository feedbackRepo, SplitDayRepository splitRepo,
                          PlanDayRepository planDayRepo) {
        this.splitRepo = splitRepo;
        this.planDayRepo = planDayRepo;
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.logRepo = logRepo;
        this.messageRepo = messageRepo;
        this.contentRepo = contentRepo;
        this.feedbackRepo = feedbackRepo;
    }

    @Transactional
    public void deletePlan(WorkoutPlan plan) {
        for (ProgressLog log : logRepo.findByPlan(plan)) {
            log.setPlan(null);
            logRepo.save(log);
        }
        enrollmentRepo.deleteByPlan(plan);
        splitRepo.deleteBySourcePlanId(plan.getId());
        planDayRepo.deleteByPlan(plan);
        planRepo.delete(plan);
    }

    @Transactional
    public void deleteUser(AppUser user) {
        planRepo.findByCoach(user).forEach(this::deletePlan);
        planRepo.findByAssignedTo(user).forEach(this::deletePlan);
        splitRepo.deleteByUser(user);
        enrollmentRepo.deleteByUser(user);
        logRepo.deleteByUser(user);
        messageRepo.deleteBySenderOrRecipient(user, user);
        contentRepo.deleteByAuthor(user);
        feedbackRepo.deleteByUser(user);
        userRepo.delete(user);
    }
}
