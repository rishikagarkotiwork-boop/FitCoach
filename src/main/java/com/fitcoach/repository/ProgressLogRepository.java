package com.fitcoach.repository;

import com.fitcoach.model.AppUser;
import com.fitcoach.model.ProgressLog;
import com.fitcoach.model.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressLogRepository extends JpaRepository<ProgressLog, Long> {
    List<ProgressLog> findByUserOrderByLogDateDescIdDesc(AppUser user);

    List<ProgressLog> findByPlan(WorkoutPlan plan);

    long countByPlan(WorkoutPlan plan);

    void deleteByUser(AppUser user);
}
