package com.saintplus.course.repository;

import com.saintplus.course.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    List<Course> findByCourseCodeStartingWith(String prefix);
    List<Course> findBySemesterIn(List<Integer> semesters);

    @Query("SELECT cm FROM Course cm WHERE cm.courseCode LIKE :prefix% AND cm.semester IN :semesters")
    List<Course> findByCourseCodeStartingWithAndSemesterIn(@Param("prefix") String prefix, @Param("semesters") List<Integer> semesters);

    @Query("SELECT cm FROM Course cm WHERE cm.semester IN :semesters")
    List<Course> findAllBySemesterIn(@Param("semesters") List<Integer> semesters);
}
