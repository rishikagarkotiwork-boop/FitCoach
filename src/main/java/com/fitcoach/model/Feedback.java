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
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser user;

    @Column(length = 1000, nullable = false)
    private String text;

    private int rating;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Feedback(AppUser user, String text, int rating) {
        this.user = user;
        this.text = text;
        this.rating = rating;
    }
}
