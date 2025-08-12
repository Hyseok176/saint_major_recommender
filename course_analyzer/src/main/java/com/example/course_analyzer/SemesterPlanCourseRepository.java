package com.example.course_analyzer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterPlanCourseRepository extends JpaRepository<SemesterPlanCourse, Long> {

    List<SemesterPlanCourse> findByUser(User user);

    Optional<SemesterPlanCourse> findByUserAndCourseMapping(User user, CourseMapping courseMapping);

    void deleteByUserAndCourseMapping(User user, CourseMapping courseMapping);
}
