package com.fitcoach.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProgressLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser user;

    @ManyToOne
    private WorkoutPlan plan;

    @Column(nullable = false)
    private String workoutName;

    private LocalDate logDate = LocalDate.now();

    private int durationMinutes;

    private Integer caloriesBurned;

    private Double weightKg;

    @Column(length = 500)
    private String notes;
}
