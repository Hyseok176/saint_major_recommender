package com.example.course_analyzer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "semester_plan_courses", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "course_mapping_id"})
})
public class SemesterPlanCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_mapping_course_code", referencedColumnName = "courseCode", nullable = false)
    private CourseMapping courseMapping;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SemesterPlanCourse(User user, CourseMapping courseMapping) {
        this.user = user;
        this.courseMapping = courseMapping;
    }
}
