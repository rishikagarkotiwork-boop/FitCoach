package com.fitcoach.repository;

import com.fitcoach.model.AppUser;
import com.fitcoach.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findTop10ByOrderByCreatedAtDesc();

    void deleteByUser(AppUser user);
}
