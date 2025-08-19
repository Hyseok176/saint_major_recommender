package com.example.course_analyzer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SemesterCourseRepository extends JpaRepository<SemesterCourse, Long> {
    List<SemesterCourse> findByUser(User user);
    List<SemesterCourse> findByCourseCode(String courseCode);

    @Query("SELECT COUNT(DISTINCT sc.user.id) FROM SemesterCourse sc WHERE sc.courseCode = :courseCode")
    long countDistinctUsersByCourseCode(@Param("courseCode") String courseCode);

    void deleteByUser(User user);
}
