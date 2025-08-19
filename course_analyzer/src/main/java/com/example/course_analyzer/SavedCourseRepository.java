package com.example.course_analyzer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {
    List<SavedCourse> findByUser(User user);
    boolean existsByUserAndCourseCode(User user, String courseCode);
    @Transactional
    void deleteByUserAndCourseCode(User user, String courseCode);
}
