package com.saintplus.course.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.saintplus.course.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saintplus.course.domain.SavedCourse;
import com.saintplus.course.service.CourseService;
import com.saintplus.user.domain.User;
import com.saintplus.user.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * CourseController
 *
 * 과목 조회, 통계, 장바구니 등 핵심 과목 데이터와 관련된 요청을 처리하는 REST 컨트롤러입니다.
 * 모든 응답은 JSON 형식으로 반환됩니다.
 */
@RestController
@RequiredArgsConstructor
public class CourseController {

    private final RecommendationService recommendationService;
    private final CourseService courseService;
    private final UserService userService;


    /**
     * 사용자 맞춤 과목 추천 (통계 기반 or AI 기반)
     *
     * URL: /api/recommendations
     */
    @GetMapping("/api/recommendations")
    public ResponseEntity<?> getRecommendations(
            Authentication authentication,
            @RequestParam(required = false) Integer semester
    ) {
        User user = userService.getUserFromAuthentication(authentication);

        // 지금은 cart / dismissed 비워서 호출
        List<String> recommendations =
                recommendationService.getRecommendCourses(user.getId());

        return ResponseEntity.ok(recommendations);
    }


    /**
     * 수강 결과 데이터를 반환합니다.
     *
     * URL: /results
     */
    @GetMapping("/results")
    public ResponseEntity<Map<String, Object>> getResults(Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        Map<String, Object> response = courseService.getResultsData(user);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 과목 조회 및 필터링 데이터를 반환합니다.
     *
     * URL: /all-courses
     */
    @GetMapping("/all-courses")
    public ResponseEntity<Map<String, Object>> getAllCourses(
            @RequestParam(value = "major", required = false) String major,
            @RequestParam(value = "semester", required = false) Integer semester,
            Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        
        Map<String, Object> response = courseService.getAllCoursesData(user, major, semester);
        response.putAll(generateFutureSemesterOptions(user));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 과목의 통계 데이터를 조회합니다.
     *
     * URL: /api/course-stats/{subjectCode}
     */
    @GetMapping("/api/course-stats/{subjectCode}")
    public ResponseEntity<Map<String, Object>> getCourseStats(@PathVariable String subjectCode) {
        try {
            Map<String, Object> stats = courseService.getCourseStats(subjectCode);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 사용자의 장바구니(담은 과목) 목록을 조회합니다.
     *
     * URL: /api/saved-courses
     */
    @GetMapping("/api/saved-courses")
    public ResponseEntity<List<SavedCourse>> getSavedCourses(Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        List<SavedCourse> savedCourses = courseService.getSavedCourses(user.getUsername());
        return ResponseEntity.ok(savedCourses);
    }

    /**
     * 장바구니에 과목을 추가합니다.
     *
     * URL: /api/saved-courses/{courseCode} (POST)
     */
    @PostMapping("/api/saved-courses/{courseCode}")
    public ResponseEntity<?> addSavedCourse(@PathVariable String courseCode, @RequestBody Map<String, String> payload,
            Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        String courseName = payload.get("courseName");
        String targetSemester = payload.get("targetSemester");
        try {
            SavedCourse savedCourse = courseService.addSavedCourse(user.getUsername(), courseCode, courseName,
                    targetSemester);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCourse);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * 장바구니에서 과목을 삭제합니다.
     *
     * URL: /api/saved-courses/{courseCode} (DELETE)
     */
    @DeleteMapping("/api/saved-courses/{courseCode}")
    public ResponseEntity<?> deleteSavedCourse(@PathVariable String courseCode, Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        try {
            courseService.deleteSavedCourse(user.getUsername(), courseCode);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // --- Helper Methods ---

    private Map<String, Object> generateFutureSemesterOptions(User user) {
        int startYear;
        int startSemester;

        String lastSemester = user.getLastSemester(); // e.g., "2025-2"

        if (lastSemester != null && lastSemester.matches("\\d{4}-[12SW]")) {
            String[] parts = lastSemester.split("-");
            int lastYear = Integer.parseInt(parts[0]);
            String lastSem = parts[1];

            if (lastSem.equals("2") || lastSem.equals("W")) {
                startYear = lastYear + 1;
                startSemester = 1;
            } else { // "1" or "S"
                startYear = lastYear;
                startSemester = 2;
            }
        } else {
            LocalDate today = LocalDate.now();
            startYear = today.getYear();
            int currentMonth = today.getMonthValue();
            startSemester = (currentMonth >= 3 && currentMonth <= 8) ? 2 : 1;
            if (startSemester == 1 && currentMonth > 8) {
                startYear++;
            }
        }

        List<Integer> years = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            years.add(startYear + i);
        }

        return Map.of("futureYears", years, "startSemester", startSemester);
    }
}
