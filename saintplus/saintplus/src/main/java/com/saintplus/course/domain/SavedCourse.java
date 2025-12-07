package com.saintplus.course.domain;

import com.saintplus.user.domain.User;
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

    @Column(name = "target_semester")
    private String targetSemester;

    public SavedCourse(User user, String courseCode, String courseName, String targetSemester) {
        this.user = user;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.targetSemester = targetSemester;
    }

    // 기존 코드를 위한 호환성 생성자
    public SavedCourse(User user, String courseCode, String courseName) {
        this(user, courseCode, courseName, null);
    }
}
