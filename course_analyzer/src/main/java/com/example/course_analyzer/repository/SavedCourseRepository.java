package com.example.course_analyzer.repository;

import com.example.course_analyzer.domain.User;
import com.example.course_analyzer.domain.SavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {
    List<SavedCourse> findByUser(User user);
    boolean existsByUserAndCourseCode(User user, String courseCode);
    long countByUserAndTargetSemester(User user, String targetSemester);
    @Transactional
    void deleteByUserAndCourseCode(User user, String courseCode);
}
