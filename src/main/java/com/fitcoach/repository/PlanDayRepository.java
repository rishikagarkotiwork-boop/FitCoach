package com.fitcoach.repository;

import com.fitcoach.model.PlanDay;
import com.fitcoach.model.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanDayRepository extends JpaRepository<PlanDay, Long> {
    List<PlanDay> findByPlanOrderByDayAsc(WorkoutPlan plan);

    void deleteByPlan(WorkoutPlan plan);
}
