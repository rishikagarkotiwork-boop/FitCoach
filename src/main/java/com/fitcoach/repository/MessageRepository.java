package com.fitcoach.repository;

import com.fitcoach.model.AppUser;
import com.fitcoach.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderOrRecipientOrderBySentAtDesc(AppUser sender, AppUser recipient);

    void deleteBySenderOrRecipient(AppUser sender, AppUser recipient);
}
