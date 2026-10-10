package com.fitcoach.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class WorkoutPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private String difficulty;

    private int durationWeeks;

    /** One exercise per line. */
    @Column(length = 2000)
    private String exercises;

    @Column(length = 1000)
    private String schedule;

    @ManyToOne(optional = false)
    private AppUser coach;

    /** When set, this is a personalised plan visible only to that user. */
    @ManyToOne
    private AppUser assignedTo;

    private LocalDate createdOn = LocalDate.now();

    public List<String> getExerciseList() {
        if (exercises == null || exercises.isBlank()) {
            return List.of();
        }
        return Arrays.stream(exercises.split("\\R")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
