package com.example.course_analyzer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseMappingRepository extends JpaRepository<CourseMapping, String> {
}
