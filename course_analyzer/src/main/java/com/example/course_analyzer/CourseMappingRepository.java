package com.example.course_analyzer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;

@Repository
public interface CourseMappingRepository extends JpaRepository<CourseMapping, String> {
    List<CourseMapping> findByCourseCodeStartingWith(String prefix);

    @Query("SELECT cm FROM CourseMapping cm WHERE cm.courseCode NOT LIKE '%ETS%' AND cm.courseCode NOT LIKE '%COR%' AND cm.courseCode NOT LIKE '%SHS%' AND cm.courseCode NOT LIKE '%HLU%'")
    List<CourseMapping> findAllExcludingCodes();
}
