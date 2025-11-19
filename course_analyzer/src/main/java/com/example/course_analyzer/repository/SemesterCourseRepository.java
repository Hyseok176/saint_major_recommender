package com.example.course_analyzer.repository;

import com.example.course_analyzer.domain.User;
import com.example.course_analyzer.domain.SemesterCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SemesterCourseRepository extends JpaRepository<SemesterCourse, Long> {
    List<SemesterCourse> findByUser(User user);
    List<SemesterCourse> findByCourseCode(String courseCode);

    @Query("SELECT COUNT(DISTINCT sc.user.id) FROM SemesterCourse sc WHERE sc.courseCode = :courseCode")
    long countDistinctUsersByCourseCode(@Param("courseCode") String courseCode);

    @Query("SELECT sc FROM SemesterCourse sc WHERE sc.courseCode = :courseCode AND sc.user.major1 = :major1")
    List<SemesterCourse> findByCourseCodeAndUserMajor1(@Param("courseCode") String courseCode, @Param("major1") String major1);

    void deleteByUser(User user);
}