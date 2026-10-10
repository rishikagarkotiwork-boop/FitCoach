package com.fitcoach.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;

/** One training day of a workout plan; copied into the user's weekly split when they join the plan. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class PlanDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private WorkoutPlan plan;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek day;

    @Column(nullable = false)
    private String focus;

    @Column(length = 2000)
    private String details;

    public PlanDay(WorkoutPlan plan, DayOfWeek day, String focus, String details) {
        this.plan = plan;
        this.day = day;
        this.focus = focus;
        this.details = details;
    }
}
