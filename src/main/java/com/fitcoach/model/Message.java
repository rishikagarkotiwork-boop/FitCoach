package com.fitcoach.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser sender;

    @ManyToOne(optional = false)
    private AppUser recipient;

    @Column(length = 1000, nullable = false)
    private String content;

    private LocalDateTime sentAt = LocalDateTime.now();

    public Message(AppUser sender, AppUser recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }
}
