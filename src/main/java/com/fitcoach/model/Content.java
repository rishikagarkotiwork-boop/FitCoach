package com.fitcoach.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** User-generated content (tips, success stories) awaiting admin moderation. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String body;

    @ManyToOne(optional = false)
    private AppUser author;

    @Enumerated(EnumType.STRING)
    private ContentStatus status = ContentStatus.PENDING;

    private LocalDate submittedOn = LocalDate.now();

    public Content(String title, String body, AppUser author) {
        this.title = title;
        this.body = body;
        this.author = author;
    }
}
