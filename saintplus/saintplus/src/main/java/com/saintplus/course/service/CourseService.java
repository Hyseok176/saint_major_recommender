package com.saintplus.course.service;

import com.saintplus.course.domain.Course;
import com.saintplus.course.dto.*;
import com.saintplus.transcript.domain.Enrollment;
import com.saintplus.course.repository.CourseRepository;
import com.saintplus.transcript.repository.EnrollmentRepository;
import com.saintplus.course.domain.SavedCourse;
import com.saintplus.course.repository.SavedCourseRepository;
import com.saintplus.user.domain.User;
import com.saintplus.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import java.util.Collections;

/**
 * CourseService
 *
 * 이 서비스 클래스는 과목 분석, 추천, 장바구니 관리 등
 * 애플리케이션의 핵심 비즈니스 로직을 담당합니다.
 *
 * 주요 기능:
 * 1. 사용자 맞춤형 과목 추천 (전공 및 교양)
 * 2. 과목 통계 데이터 제공
 * 3. 장바구니(담은 과목) 관리
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    // 필수 교양 트랙 정보 (트랙 번호 -> 과목 코드 리스트)
    private static final Map<Integer, List<String>> GE_TRACKS;
    // 과목 코드 -> 트랙 이름 매핑 (추천 결과 표시용)
    private static final Map<String, String> COURSE_CODE_TO_TRACK_NAME_MAP;
    // FastAPI 서버 주소
    private static final String AI_SERVER_URL = "http://localhost:8000/recommend";

    static {
        Map<Integer, List<String>> tracks = new HashMap<>();
        tracks.put(1, List.of("HFS2001", "HFS2002", "HFS2003", "HFU4012", "HFU4023")); // 인간과 신앙
        tracks.put(2, List.of("ETS2001", "ETS2002", "ETS2004", "CHS2002", "CHS2003", "CHS2004", "HSS3032")); // 인간과 사상
        tracks.put(3, List.of("SHS2001", "SHS2002", "SHS2003", "SHS2007", "SHS2005")); // 인간과 사회
        tracks.put(4, List.of("STS2001", "STS2002", "STU4011", "STS2011", "STS2012", "STS2010", "STS2005", "STS2015")); // 인간과 과학
        tracks.put(5, List.of("COR1003", "LCS2001", "LCS2003", "LCS2005", "LCS2007", "LCU4021", "LCU4025", "LCU4030", "LCU4035", "LCU4105")); // 글로벌 언어
        GE_TRACKS = Collections.unmodifiableMap(tracks);

        Map<Integer, String> trackNames = new HashMap<>();
        trackNames.put(1, "인간과 신앙");
        trackNames.put(2, "인간과 사상");
        trackNames.put(3, "인간과 사회");
        trackNames.put(4, "인간과 과학&AI");
        trackNames.put(5, "글로벌 언어");

        Map<String, String> reverseMap = new HashMap<>();
        for (Map.Entry<Integer, List<String>> entry : GE_TRACKS.entrySet()) {
            Integer trackNumber = entry.getKey();
            String trackName = trackNames.get(trackNumber);
            String formattedTrackInfo = String.format("- %d트랙 %s", trackNumber, trackName);
            for (String courseCode : entry.getValue()) {
                reverseMap.put(courseCode, formattedTrackInfo);
            }
        }
        COURSE_CODE_TO_TRACK_NAME_MAP = Collections.unmodifiableMap(reverseMap);
    }

    private final EnrollmentRepository enrollmentRepository;

    private final CourseRepository courseRepository;

    private final SavedCourseRepository savedCourseRepository;

    private final UserRepository userRepository;



    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

     /**
     * AI 모델(Python FastAPI)을 통해 문맥 기반 과목 추천을 수행합니다.
     * * @param prompt 사용자의 질문 (예: "데이터 분석과 관련된 수업")
     * @param major 대상 전공 코드 (예: "CSE")
     * @return 추천된 과목 코드 및 유사도 점수 리스트
     */
    public List<RecommendedCourseDto> getAiRecommendedCourses(String prompt, String major) {
        RestTemplate restTemplate = new RestTemplate();
        
        // 1. 파이썬 서버로 보낼 요청 객체 생성
        AiRecommendRequest request = new AiRecommendRequest(prompt, major, 0.25);
        
        try {
            // 2. 파이썬 서버에 POST 요청
            AiRecommendResponse aiResponse = restTemplate.postForObject(AI_SERVER_URL, request, AiRecommendResponse.class);
            
            if (aiResponse == null || aiResponse.getResults() == null) {
                return Collections.emptyList();
            }

            // 3. 반환된 과목 코드를 바탕으로 DB에서 상세 정보 조회 및 DTO 변환
            return aiResponse.getResults().stream()
                    .map(item -> {
                        Course course = courseRepository.findById(item.getCode()).orElse(null);
                        if (course == null) return null;

                        return RecommendedCourseDto.builder()
                                .course(course)
                                .score(item.getScore()) // AI가 계산한 유사도 점수
                                .studentCount((int) enrollmentRepository.countDistinctUsersByCourseCode(course.getCourseCode()))
                                .build();
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // 서버 미가동 시 빈 리스트 반환
            System.err.println("AI 추천 서버 통신 실패: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 특정 과목의 학기별 수강생 수 통계를 반환합니다.
     * 1학기부터 8학기까지의 분포를 계산합니다.
     */
    public Map<String, Object> getCourseStats(String subjectCode) {
        List<Enrollment> courses = enrollmentRepository.findByCourseCode(subjectCode);
        Map<Double, Long> semesterCounts = courses.stream().collect(Collectors.groupingBy(Enrollment::getSemester, Collectors.counting()));
        Map<Double, Long> allSemesters = new LinkedHashMap<>();
        
        // 1~8학기 기본값 0으로 초기화
        for (int i = 1; i <= 8; i++) {
            allSemesters.put((double) i, 0L);
        }
        semesterCounts.forEach(allSemesters::put);
        
        Map<Double, Long> sortedSemesterCounts = new LinkedHashMap<>();
        allSemesters.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEachOrdered(x -> sortedSemesterCounts.put(x.getKey(), x.getValue()));
        
        List<String> labels = sortedSemesterCounts.keySet().stream().map(semester -> String.format(semester % 1 == 0 ? "%.0f학기" : "%.1f학기", semester)).collect(Collectors.toList());
        List<Long> values = new ArrayList<>(sortedSemesterCounts.values());
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("labels", labels);
        stats.put("values", values);
        return stats;
    }

    /**
     * 특정 전공의 과목들을 수강생 수 기준으로 정렬하여 반환합니다.
     */
    public List<CourseStatDto> getCoursesByMajor(String majorPrefix, Integer semester) {
        List<Integer> targetSemesters = new ArrayList<>(List.of(3, 4)); // 공통 과목 및 미분류 과목 포함
        if (semester != null) {
            targetSemesters.add(semester);
        } else {
            targetSemesters.addAll(List.of(1, 2)); // 학기 미선택 시 전체
        }
        List<Course> courses = courseRepository.findByCourseCodeStartingWithAndSemesterIn(majorPrefix, targetSemesters);
        return courses.stream()
                .map(course -> new CourseStatDto(course.getCourseCode(), course.getCourseName(), enrollmentRepository.countDistinctUsersByCourseCode(course.getCourseCode())))
                .sorted(Comparator.comparingLong(CourseStatDto::getTotalStudentCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 전공이 아닌 과목(비전공/교양)을 조회합니다.
     */
    public List<Course> getNonMajorCourses(User user, Integer semester) {
        List<Integer> targetSemesters = new ArrayList<>(List.of(3, 4));
        if (semester != null) {
            targetSemesters.add(semester);
        } else {
            targetSemesters.addAll(List.of(1, 2));
        }
        List<Course> allCoursesInSemester = courseRepository.findAllBySemesterIn(targetSemesters);
        
        List<String> userMajorPrefixes = new ArrayList<>();
        if (user.getMajor1() != null && !user.getMajor1().isEmpty() && !user.getMajor1().equals("미선택")) {
            userMajorPrefixes.add(getCoursePrefixForMajor(user.getMajor1()));
        }
        if (user.getMajor2() != null && !user.getMajor2().isEmpty() && !user.getMajor2().equals("미선택")) {
            userMajorPrefixes.add(getCoursePrefixForMajor(user.getMajor2()));
        }
        if (user.getMajor3() != null && !user.getMajor3().isEmpty() && !user.getMajor3().equals("미선택")) {
            userMajorPrefixes.add(getCoursePrefixForMajor(user.getMajor3()));
        }

        return allCoursesInSemester.stream()
                .filter(course -> userMajorPrefixes.stream().noneMatch(prefix -> course.getCourseCode().startsWith(prefix)))
                .collect(Collectors.toList());
    }

    private int getCurrentSemester(User user) {
        OptionalDouble maxSemester = enrollmentRepository.findByUser(user).stream()
                .mapToDouble(Enrollment::getSemester)
                .max();
        return (int) Math.ceil(maxSemester.orElse(1.0));
    }

    /**
     * 사용자에게 적합한 과목을 추천합니다.
     * 1. 전공 추천: 사용자의 전공 과목 중, 현재 학기와 가까운 시기에 다른 학생들이 많이 수강한 과목
     * 2. 교양 추천: 필수 교양 트랙 중 미이수한 트랙 우선 추천, 모두 이수 시 일반 교양 추천
     *
     * @param user 사용자
     * @param cartCourseCodes 장바구니에 담긴 과목 (추천 제외)
     * @param dismissedCourseCodes 추천 제외 목록
     * @param semester 대상 학기 (1 또는 2)
     * @return 전공 및 교양 추천 목록 맵
     */
    public Map<String, List<RecommendedCourseDto>> recommendCourses(User user, List<String> cartCourseCodes, List<String> dismissedCourseCodes, Integer semester) {
        int currentUserSemester = getCurrentSemester(user);

        List<Integer> targetSemesters = new ArrayList<>();
        if (semester != null) {
            targetSemesters.add(semester); // 1 or 2
        }
        targetSemesters.add(3); // Always include common courses
        targetSemesters.add(4); // Always include courses with no specific semester

        List<String> userMajorPrefixes = new ArrayList<>();
        if (user.getMajor1() != null && !user.getMajor1().isEmpty() && !user.getMajor1().equals("미선택")) {
            userMajorPrefixes.add(getCoursePrefixForMajor(user.getMajor1()));
        }
        if (user.getMajor2() != null && !user.getMajor2().isEmpty() && !user.getMajor2().equals("미선택")) {
            userMajorPrefixes.add(getCoursePrefixForMajor(user.getMajor2()));
        }
        if (user.getMajor3() != null && !user.getMajor3().isEmpty() && !user.getMajor3().equals("미선택")) {
            userMajorPrefixes.add(getCoursePrefixForMajor(user.getMajor3()));
        }

        // 현재 학기에 맞는 과목만 DB에서 조회
        List<Course> allCourses = courseRepository.findBySemesterIn(targetSemesters);
        List<String> userTakenCourseCodes = enrollmentRepository.findByUser(user).stream()
                .map(Enrollment::getCourseCode)
                .toList();

        // --- 전공 추천 로직 (Major Recommendations) ---
        List<RecommendedCourseDto> majorRecommendations = new ArrayList<>();
        if (!userMajorPrefixes.isEmpty()) {
            // 접두사 -> 전공명 매핑
            Map<String, String> prefixToMajorNameMap = new HashMap<>();
            if (user.getMajor1() != null && !user.getMajor1().isEmpty() && !user.getMajor1().equals("미선택")) prefixToMajorNameMap.put(getCoursePrefixForMajor(user.getMajor1()), user.getMajor1());
            if (user.getMajor2() != null && !user.getMajor2().isEmpty() && !user.getMajor2().equals("미선택")) prefixToMajorNameMap.put(getCoursePrefixForMajor(user.getMajor2()), user.getMajor2());
            if (user.getMajor3() != null && !user.getMajor3().isEmpty() && !user.getMajor3().equals("미선택")) prefixToMajorNameMap.put(getCoursePrefixForMajor(user.getMajor3()), user.getMajor3());

            majorRecommendations = allCourses.stream()
                    .filter(course -> userMajorPrefixes.stream().anyMatch(prefix -> !prefix.isEmpty() && course.getCourseCode().startsWith(prefix)))
                    .filter(course -> !userTakenCourseCodes.contains(course.getCourseCode()))
                    .filter(course -> !cartCourseCodes.contains(course.getCourseCode()))
                    .filter(course -> !dismissedCourseCodes.contains(course.getCourseCode()))
                    .map(course -> {
                        String majorName = prefixToMajorNameMap.entrySet().stream()
                                .filter(entry -> course.getCourseCode().startsWith(entry.getKey()))
                                .map(Map.Entry::getValue)
                                .findFirst()
                                .orElse(null);

                        if (course.getSemester() != null && course.getSemester() == 4) {
                            return RecommendedCourseDto.builder()
                                    .course(course)
                                    .score(0.01)
                                    .studentCount(0)
                                    .averageProximityScore(0)
                                    .majorName(majorName)
                                    .build();
                        }
                        List<Enrollment> allTakes = enrollmentRepository.findByCourseCode(course.getCourseCode());
                        // 점수 계산: (1 / (1 + |내 학기 - 수강생 학기|)) 의 합
                        // 즉, 나와 비슷한 학기에 수강한 사람이 많을수록 점수가 높음
                        double score = allTakes.stream()
                                .mapToDouble(sc -> 1.0 / (1.0 + Math.abs(currentUserSemester - sc.getSemester())))
                                .sum();
                        double averageProximity = allTakes.isEmpty() ? 0 : score / allTakes.size();
                        return RecommendedCourseDto.builder()
                                .course(course)
                                .score(score)
                                .studentCount(allTakes.size())
                                .averageProximityScore(averageProximity)
                                .majorName(majorName)
                                .build();
                    })
                    .sorted(Comparator.comparingDouble(RecommendedCourseDto::getScore).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
        }

        // --- 교양 추천 로직 (GE Recommendations) ---
        List<RecommendedCourseDto> geRecommendations;

        // 1. 미이수 트랙 확인
        List<Integer> uncompletedTracks = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : GE_TRACKS.entrySet()) {
            boolean completed = userTakenCourseCodes.stream()
                    .anyMatch(takenCode -> entry.getValue().contains(takenCode));
            if (!completed) {
                uncompletedTracks.add(entry.getKey());
            }
        }

        // 2. 트랙 이수 여부에 따른 추천 분기
        if (!uncompletedTracks.isEmpty()) {
            // Case 1: 미이수 트랙이 있는 경우 -> 해당 트랙 과목 추천
            List<String> codesForUncompletedTracks = uncompletedTracks.stream()
                    .flatMap(trackNum -> GE_TRACKS.get(trackNum).stream())
                    .toList();

            geRecommendations = allCourses.stream()
                    .filter(course -> codesForUncompletedTracks.contains(course.getCourseCode()))
                    .filter(course -> !userTakenCourseCodes.contains(course.getCourseCode()))
                    .filter(course -> !cartCourseCodes.contains(course.getCourseCode()))
                    .filter(course -> !dismissedCourseCodes.contains(course.getCourseCode()))
                    .map(course -> {
                        String trackName = COURSE_CODE_TO_TRACK_NAME_MAP.get(course.getCourseCode());
                        int studentCount = enrollmentRepository.findByCourseCode(course.getCourseCode()).size();
                        return RecommendedCourseDto.builder()
                                .course(course)
                                .score(0)
                                .studentCount(studentCount)
                                .averageProximityScore(0)
                                .trackName(trackName)
                                .build();
                    })
                    .sorted(Comparator.comparingInt(RecommendedCourseDto::getStudentCount).reversed()) // 수강생 많은 순
                    .limit(5)
                    .collect(Collectors.toList());
        } else {
            // Case 2: 모든 트랙 이수 완료 -> 일반 교양 추천 (전공 제외)
            List<String> allMajorPrefixes = List.of("MAT", "PHY", "CHM", "BIO", "EEE", "MEE", "CSE", "CBE", "SSE", "AIE", "ECO", "MGT", "EDU");
            final boolean useMajorFilteredRecommendations = user.getMajor1() != null && !user.getMajor1().isEmpty() && !user.getMajor1().equals("미선택");

            geRecommendations = allCourses.stream()
                    .filter(course -> allMajorPrefixes.stream().noneMatch(prefix -> course.getCourseCode().startsWith(prefix)))
                    .filter(course -> !userTakenCourseCodes.contains(course.getCourseCode()))
                    .filter(course -> !cartCourseCodes.contains(course.getCourseCode()))
                    .filter(course -> !dismissedCourseCodes.contains(course.getCourseCode()))
                    .map(course -> {
                        if (course.getSemester() != null && course.getSemester() == 4) {
                            return RecommendedCourseDto.builder()
                                    .course(course)
                                    .score(0.01)
                                    .studentCount(0)
                                    .averageProximityScore(0)
                                    .build();
                        }
                        List<Enrollment> allTakes;
                        if (useMajorFilteredRecommendations) {
                            allTakes = enrollmentRepository.findByCourseCodeAndUserMajor1(course.getCourseCode(), user.getMajor1());
                        } else {
                            allTakes = enrollmentRepository.findByCourseCode(course.getCourseCode());
                        }
                        double score = allTakes.stream()
                                .mapToDouble(sc -> 1.0 / (1.0 + Math.abs(currentUserSemester - sc.getSemester())))
                                .sum();
                        double averageProximity = allTakes.isEmpty() ? 0 : score / allTakes.size();
                        return RecommendedCourseDto.builder()
                                .course(course)
                                .score(score)
                                .studentCount(allTakes.size())
                                .averageProximityScore(averageProximity)
                                .build();
                    })
                    .sorted(Comparator.comparingDouble(RecommendedCourseDto::getScore).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
        }

        Map<String, List<RecommendedCourseDto>> recommendationsMap = new HashMap<>();
        recommendationsMap.put("major", majorRecommendations);
        recommendationsMap.put("ge", geRecommendations);

        return recommendationsMap;
    }

    /**
     * 전공 한글명에 해당하는 과목 코드 접두사를 반환합니다.
     */
    public String getCoursePrefixForMajor(String major) {
        switch (major) {
            case "수학": return "MAT";
            case "물리학": return "PHY";
            case "화학": return "CHM";
            case "생명과학": return "BIO";
            case "전자공학": return "EEE";
            case "기계공학": return "MEE";
            case "컴퓨터공학": return "CSE";
            case "화공생명공학": return "CBE";
            case "시스템반도체공학": return "SSE";
            case "인공지능학과": return "AIE";
            case "경제학": return "ECO";
            case "경영학": return "MGT";
            case "교육문화": return "EDU";
            default: return "";
        }
    }

    @Transactional(readOnly = true)
    public List<SavedCourse> getSavedCourses(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return savedCourseRepository.findByUser(user);
    }

    @Transactional
    public SavedCourse addSavedCourse(String username, String courseCode, String courseName, String targetSemester) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        // 한 학기 최대 8과목 제한
        if (savedCourseRepository.countByUserAndTargetSemester(user, targetSemester) >= 8) {
            throw new IllegalStateException("한 학기에는 최대 8과목까지 담을 수 있습니다.");
        }
        if (savedCourseRepository.existsByUserAndCourseCode(user, courseCode)) {
            throw new IllegalStateException("이미 장바구니에 담긴 과목입니다.");
        }
        SavedCourse savedCourse = new SavedCourse(user, courseCode, courseName, targetSemester);
        return savedCourseRepository.save(savedCourse);
    }

    @Transactional
    public void deleteSavedCourse(String username, String courseCode) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        savedCourseRepository.deleteByUserAndCourseCode(user, courseCode);
    }

    /**
     * 사용자가 필수 교양 트랙 중 미이수한 트랙이 있는지 확인합니다.
     */
    public boolean hasUncompletedTracks(User user) {
        List<String> userTakenCourseCodes = enrollmentRepository.findByUser(user).stream()
                .map(Enrollment::getCourseCode)
                .toList();

        for (Map.Entry<Integer, List<String>> entry : GE_TRACKS.entrySet()) {
            boolean completed = userTakenCourseCodes.stream()
                    .anyMatch(takenCode -> entry.getValue().contains(takenCode));
            if (!completed) {
                return true; // Found an uncompleted track
            }
        }
        return false; // All tracks completed
    }

    /**
     * 사용자의 수강 결과 데이터를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getResultsData(User user) {
        // 1. 수강 완료한 과목 조회 및 처리
        List<Enrollment> takenCourses = enrollmentRepository.findByUser(user).stream()
                .filter(c -> c.getSemester() > 0)
                .toList();

        // 학기별로 과목 그룹화
        Map<Double, List<CourseAnalysisData>> coursesBySemesterNumber = takenCourses.stream()
                .collect(Collectors.groupingBy(Enrollment::getSemester,
                        java.util.TreeMap::new,
                        Collectors.mapping(sc -> {
                            String courseCode = sc.getCourseCode();
                            String actualCourseName = courseRepository.findById(courseCode)
                                    .map(Course::getCourseName)
                                    .orElse(courseCode);
                            return new CourseAnalysisData(String.valueOf(sc.getSemester()), courseCode, actualCourseName);
                        }, Collectors.toList())));

        Map<String, List<CourseAnalysisData>> coursesForModel = new LinkedHashMap<>();

        coursesBySemesterNumber.forEach((semester, courses) -> {
            String semesterKey = (semester % 1 == 0)
                    ? String.format("%.0f학기", semester)
                    : String.format("%.1f학기", semester);
            coursesForModel.put(semesterKey, courses);
        });

        // 2. 장바구니(계획) 과목 조회 및 처리
        List<SavedCourse> savedCourses = getSavedCourses(user.getUsername());
        Map<String, List<SavedCourse>> savedCoursesBySemester = savedCourses.stream()
                .filter(sc -> sc.getTargetSemester() != null && !sc.getTargetSemester().isEmpty())
                .collect(Collectors.groupingBy(SavedCourse::getTargetSemester));

        List<String> sortedFutureSemesters = savedCoursesBySemester.keySet().stream().sorted()
                .collect(Collectors.toList());

        for (String semester : sortedFutureSemesters) {
            String semesterKey = String.format("%s (계획)", semester);
            List<CourseAnalysisData> courseViewModels = savedCoursesBySemester.get(semester).stream()
                    .map(sc -> new CourseAnalysisData(null, sc.getCourseCode(), sc.getCourseName(), "담은 과목"))
                    .collect(Collectors.toList());
            coursesForModel.put(semesterKey, courseViewModels);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("coursesBySemester", coursesForModel);
        response.put("savedCourses", savedCourses);

        return response;
    }

    /**
     * 전체 과목 조회 및 필터링 데이터를 반환합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllCoursesData(User user, String major, Integer semester) {
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
            List<Course> nonMajorCourseMappings = getNonMajorCourses(user, semester);
            List<CourseStatDto> courses = mapToCourseStatDto(nonMajorCourseMappings);
            response.put("courses", courses);
            response.put("selectedMajor", "NonMajor");
        } else if (major != null && !major.isEmpty() && !"All".equals(major)) {
            String majorPrefix = getCoursePrefixForMajor(major);
            List<CourseStatDto> courses = getCoursesByMajor(majorPrefix, semester);
            response.put("courses", courses);
            response.put("selectedMajor", major);
        } else {
            List<Course> allCourseMappings = getAllCourses();
            List<CourseStatDto> courses = mapToCourseStatDto(allCourseMappings);
            response.put("courses", courses);
            response.put("selectedMajor", "All");
        }

        response.put("selectedSemester", semester);

        return response;
    }

    /**
     * Course 리스트를 CourseStatDto 리스트로 변환합니다.
     */
    private List<CourseStatDto> mapToCourseStatDto(List<Course> courseMappings) {
        return courseMappings.stream()
                .map(course -> new CourseStatDto(
                        course.getCourseCode(),
                        course.getCourseName(),
                        enrollmentRepository.countDistinctUsersByCourseCode(course.getCourseCode())))
                .collect(Collectors.toList());
    }
}
