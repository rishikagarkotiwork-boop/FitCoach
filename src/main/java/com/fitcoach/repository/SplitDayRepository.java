package com.fitcoach.repository;

import com.fitcoach.model.AppUser;
import com.fitcoach.model.SplitDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface SplitDayRepository extends JpaRepository<SplitDay, Long> {
    List<SplitDay> findByUser(AppUser user);

    Optional<SplitDay> findByUserAndDay(AppUser user, DayOfWeek day);

    void deleteByUser(AppUser user);

    void deleteByUserAndSourcePlanId(AppUser user, Long sourcePlanId);

    void deleteBySourcePlanId(Long sourcePlanId);
}
