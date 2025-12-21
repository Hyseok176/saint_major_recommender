package com.saintplus.course.repository;

import com.saintplus.course.domain.CourseMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseMappingRepository extends JpaRepository<CourseMapping, String> {
    List<CourseMapping> findByCourseCodeStartingWith(String prefix);
    List<CourseMapping> findBySemesterIn(List<Integer> semesters);
}
