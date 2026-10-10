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
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser user;

    @ManyToOne(optional = false)
    private WorkoutPlan plan;

    private LocalDate enrolledOn = LocalDate.now();

    public Enrollment(AppUser user, WorkoutPlan plan) {
        this.user = user;
        this.plan = plan;
    }
}
