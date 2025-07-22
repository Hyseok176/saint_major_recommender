package com.example.course_analyzer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SemesterCourseRepository extends JpaRepository<SemesterCourse, Long> {
    List<SemesterCourse> findByUser(User user);
}
