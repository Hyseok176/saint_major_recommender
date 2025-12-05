package com.saintplus.coursehistory.repository;

import com.saintplus.coursehistory.domain.Enrollment;
import com.saintplus.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByUser(User user);
    List<Enrollment> findByCourseCode(String courseCode);

    @Query("SELECT COUNT(DISTINCT sc.user.id) FROM Enrollment sc WHERE sc.courseCode = :courseCode")
    long countDistinctUsersByCourseCode(@Param("courseCode") String courseCode);

    @Query("SELECT sc FROM Enrollment sc WHERE sc.courseCode = :courseCode AND sc.user.major1 = :major1")
    List<Enrollment> findByCourseCodeAndUserMajor1(@Param("courseCode") String courseCode, @Param("major1") String major1);

    void deleteByUser(User user);
}
