package com.example.course_analyzer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SemesterCourseRepository extends JpaRepository<SemesterCourse, Long> {
    List<SemesterCourse> findByUser(User user);
    List<SemesterCourse> findByCourseCode(String courseCode);

    List<SemesterCourse> findByUserAndSemester(User user, double semester);

    boolean existsByUserAndCourseCodeAndSemester(User user, String courseCode, double semester);

    @Query("SELECT COUNT(DISTINCT sc.user.id) FROM SemesterCourse sc WHERE sc.courseCode = :courseCode")
    long countDistinctUsersByCourseCode(@Param("courseCode") String courseCode);

    @Transactional
    void deleteByUserAndCourseCodeAndSemester(User user, String courseCode, double semester);

    void deleteByUser(User user);
}
