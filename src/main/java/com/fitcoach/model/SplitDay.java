package com.fitcoach.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;

/** One day of a user's weekly workout split (e.g. Monday - Chest &amp; Triceps). */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "day_of_week"}))
@Getter
@Setter
@NoArgsConstructor
public class SplitDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek day;

    @Column(nullable = false)
    private String focus;

    @Column(length = 1000)
    private String details;

    /** Name of whoever set this day (the user themself or their coach). */
    private String setBy;

    /** Set when this day was filled in by joining a plan; used to remove it again on leaving. */
    private Long sourcePlanId;
}
