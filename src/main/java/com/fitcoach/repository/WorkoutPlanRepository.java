package com.fitcoach.repository;

import com.fitcoach.model.AppUser;
import com.fitcoach.model.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    List<WorkoutPlan> findByCoach(AppUser coach);

    List<WorkoutPlan> findByAssignedTo(AppUser user);

    List<WorkoutPlan> findByAssignedToIsNull();
}
