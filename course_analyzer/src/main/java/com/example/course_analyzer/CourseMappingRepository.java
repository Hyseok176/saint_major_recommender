package com.example.course_analyzer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseMappingRepository extends JpaRepository<CourseMapping, String> {
    List<CourseMapping> findByCourseCodeStartingWith(String prefix);
    List<CourseMapping> findBySemesterIn(List<Integer> semesters);

    @Query("SELECT cm FROM CourseMapping cm WHERE cm.courseCode LIKE :prefix% AND cm.semester IN :semesters")
    List<CourseMapping> findByCourseCodeStartingWithAndSemesterIn(@Param("prefix") String prefix, @Param("semesters") List<Integer> semesters);

    @Query("SELECT cm FROM CourseMapping cm WHERE cm.semester IN :semesters")
    List<CourseMapping> findAllBySemesterIn(@Param("semesters") List<Integer> semesters);
}