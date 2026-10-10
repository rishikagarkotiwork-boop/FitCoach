package com.fitcoach.repository;

import com.fitcoach.model.AppUser;
import com.fitcoach.model.Enrollment;
import com.fitcoach.model.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByUser(AppUser user);

    List<Enrollment> findByPlanCoach(AppUser coach);

    Optional<Enrollment> findByUserAndPlan(AppUser user, WorkoutPlan plan);

    long countByPlan(WorkoutPlan plan);

    void deleteByUser(AppUser user);

    void deleteByPlan(WorkoutPlan plan);
}
