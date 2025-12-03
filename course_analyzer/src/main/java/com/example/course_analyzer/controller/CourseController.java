package com.example.course_analyzer.controller;

import com.example.course_analyzer.domain.*;
import com.example.course_analyzer.dto.CourseStatDto;
import com.example.course_analyzer.repository.CourseMappingRepository;
import com.example.course_analyzer.repository.SemesterCourseRepository;
import com.example.course_analyzer.repository.UserRepository;
import com.example.course_analyzer.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CourseController
 *
 * 과목 조회, 통계, 장바구니 등 핵심 과목 데이터와 관련된 요청을 처리하는 REST 컨트롤러입니다.
 * 모든 응답은 JSON 형식으로 반환됩니다.
 */
@RestController
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;
    private final SemesterCourseRepository semesterCourseRepository;
    private final CourseMappingRepository courseMappingRepository;

    /**
     * 수강 결과 데이터를 반환합니다.
     *
     * URL: /results
     */
    @GetMapping("/results")
    public ResponseEntity<Map<String, Object>> getResults() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        // 1. 수강 완료한 과목 조회 및 처리
        List<SemesterCourse> takenCourses = semesterCourseRepository.findByUser(user).stream()
                .filter(c -> c.getSemester() > 0)
                .collect(Collectors.toList());

        // 학기별로 과목 그룹화
        Map<Double, List<Course>> coursesBySemesterNumber = takenCourses.stream()
                .collect(Collectors.groupingBy(SemesterCourse::getSemester,
                        java.util.TreeMap::new,
                        Collectors.mapping(sc -> {
                            String courseCode = sc.getCourseCode();
                            String actualCourseName = courseMappingRepository.findById(courseCode)
                                    .map(CourseMapping::getCourseName)
                                    .orElse(courseCode);
                            return new Course(String.valueOf(sc.getSemester()), courseCode, actualCourseName,
                                    sc.getGrade());
                        }, Collectors.toList())));

        Map<String, List<Course>> coursesForModel = new LinkedHashMap<>();

        coursesBySemesterNumber.forEach((semester, courses) -> {
            String semesterKey = (semester % 1 == 0)
                    ? String.format("%.0f학기", semester)
                    : String.format("%.1f학기", semester);
            coursesForModel.put(semesterKey, courses);
        });

        // 2. 장바구니(계획) 과목 조회 및 처리
        List<SavedCourse> savedCourses = courseService.getSavedCourses(user.getUsername());
        Map<String, List<SavedCourse>> savedCoursesBySemester = savedCourses.stream()
                .filter(sc -> sc.getTargetSemester() != null && !sc.getTargetSemester().isEmpty())
                .collect(Collectors.groupingBy(SavedCourse::getTargetSemester));

        List<String> sortedFutureSemesters = savedCoursesBySemester.keySet().stream().sorted()
                .collect(Collectors.toList());

        for (String semester : sortedFutureSemesters) {
            String semesterKey = String.format("%s (계획)", semester);
            List<Course> courseViewModels = savedCoursesBySemester.get(semester).stream()
                    .map(sc -> new Course(null, sc.getCourseCode(), sc.getCourseName(), "담은 과목"))
                    .collect(Collectors.toList());
            coursesForModel.put(semesterKey, courseViewModels);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("coursesBySemester", coursesForModel);
        response.put("savedCourses", savedCourses);

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
            @RequestParam(value = "semester", required = false) Integer semester) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        Map<String, Object> response = new HashMap<>();

        // 사용자의 전공 정보
        List<String> userMajors = new ArrayList<>();
        if (user.getMajor1() != null && !user.getMajor1().isEmpty())
            userMajors.add(user.getMajor1());
        if (user.getMajor2() != null && !user.getMajor2().isEmpty())
            userMajors.add(user.getMajor2());
        if (user.getMajor3() != null && !user.getMajor3().isEmpty())
            userMajors.add(user.getMajor3());
        response.put("userMajors", userMajors);

        // 필터링 로직
        if ("NonMajor".equals(major)) {
            List<CourseMapping> nonMajorCourseMappings = courseService.getNonMajorCourses(user, semester);
            List<CourseStatDto> courses = mapToCourseStatDto(nonMajorCourseMappings);
            response.put("courses", courses);
            response.put("selectedMajor", "NonMajor");
        } else if (major != null && !major.isEmpty() && !"All".equals(major)) {
            String majorPrefix = courseService.getCoursePrefixForMajor(major);
            List<CourseStatDto> courses = courseService.getCoursesByMajor(majorPrefix, semester);
            response.put("courses", courses);
            response.put("selectedMajor", major);
        } else {
            List<CourseMapping> allCourseMappings = courseService.getAllCourses();
            List<CourseStatDto> courses = mapToCourseStatDto(allCourseMappings);
            response.put("courses", courses);
            response.put("selectedMajor", "All");
        }

        response.putAll(generateFutureSemesterOptions(user));
        response.put("selectedSemester", semester);

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
        User user = getUserFromAuthentication(authentication);
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
        User user = getUserFromAuthentication(authentication);
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
        User user = getUserFromAuthentication(authentication);
        try {
            courseService.deleteSavedCourse(user.getUsername(), courseCode);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // --- Helper Methods ---

    private List<CourseStatDto> mapToCourseStatDto(List<CourseMapping> courseMappings) {
        return courseMappings.stream()
                .map(course -> new CourseStatDto(
                        course.getCourseCode(),
                        course.getCourseName(),
                        semesterCourseRepository.countDistinctUsersByCourseCode(course.getCourseCode())))
                .collect(Collectors.toList());
    }

    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String providerId = oauth2User.getName();
            return userRepository.findByProviderAndProviderId("kakao", providerId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with providerId: " + providerId));
        } else {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        }
    }

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
