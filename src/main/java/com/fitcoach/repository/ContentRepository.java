package com.fitcoach.repository;

import com.fitcoach.model.AppUser;
import com.fitcoach.model.Content;
import com.fitcoach.model.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {
    List<Content> findByStatus(ContentStatus status);

    long countByStatus(ContentStatus status);

    List<Content> findByAuthor(AppUser author);

    void deleteByAuthor(AppUser author);
}
