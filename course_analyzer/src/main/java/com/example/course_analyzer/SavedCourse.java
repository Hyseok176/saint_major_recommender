package com.example.course_analyzer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "saved_courses", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "course_code"})
})
public class SavedCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    public SavedCourse(User user, String courseCode, String courseName) {
        this.user = user;
        this.courseCode = courseCode;
        this.courseName = courseName;
    }
}
