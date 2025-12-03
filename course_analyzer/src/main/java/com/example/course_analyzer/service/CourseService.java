package com.example.course_analyzer.service;

import com.example.course_analyzer.domain.Course;
import com.example.course_analyzer.domain.CourseMapping;
import com.example.course_analyzer.domain.SavedCourse;
import com.example.course_analyzer.domain.SemesterCourse;
import com.example.course_analyzer.domain.User;
import com.example.course_analyzer.dto.CourseStatDto;
import com.example.course_analyzer.dto.FileAnalysisResult;
import com.example.course_analyzer.dto.RecommendationRequestDto;
import com.example.course_analyzer.dto.RecommendedCourseDto;
import com.example.course_analyzer.dto.SemesterInfo;
import com.example.course_analyzer.dto.TranscriptParsingResult;
import com.example.course_analyzer.repository.CourseMappingRepository;
import com.example.course_analyzer.repository.SavedCourseRepository;
import com.example.course_analyzer.repository.SemesterCourseRepository;
import com.example.course_analyzer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import java.util.Collections;

/**
 * CourseService
 *
 * 이 서비스 클래스는 과목 분석, 추천, 성적표 처리, 장바구니 관리 등
 * 애플리케이션의 핵심 비즈니스 로직을 담당합니다.
 *
 * 주요 기능:
 * 1. 성적표 파일 파싱 및 사용자 수강 이력 업데이트
 * 2. 사용자 맞춤형 과목 추천 (전공 및 교양)
 * 3. 과목 통계 데이터 제공
 * 4. 장바구니(담은 과목) 관리
 */
@Service
public class CourseService {

    // 필수 교양 트랙 정보 (트랙 번호 -> 과목 코드 리스트)
    private static final Map<Integer, List<String>> GE_TRACKS;
    // 과목 코드 -> 트랙 이름 매핑 (추천 결과 표시용)
    private static final Map<String, String> COURSE_CODE_TO_TRACK_NAME_MAP;

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

    @Autowired
    private SemesterCourseRepository semesterCourseRepository;

    @Autowired
    private CourseMappingRepository courseMappingRepository;

    @Autowired
    private SavedCourseRepository savedCourseRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 사용자가 업로드한 성적표 파일을 분석하여 수강 이력과 전공 정보를 업데이트합니다.
     *
     * @param user 현재 로그인한 사용자
     * @param file 업로드된 성적표 파일
     * @param major1 1전공
     * @param major2 2전공
     * @param major3 3전공
     * @param ipAddress 요청 IP 주소 (로깅용)
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    @Transactional
    public void updateUserTranscript(User user, MultipartFile file, String major1, String major2, String major3, String ipAddress) throws IOException {
        // 최초 업로드 시 가입일 설정
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(java.time.LocalDateTime.now());
        }

        // 전공 정보 업데이트 (공백 제거)
        user.setMajor1(major1.replace(" ", ""));
        user.setMajor2(major2.replace(" ", ""));
        user.setMajor3(major3.replace(" ", ""));

        // 파일 분석 실행
        FileAnalysisResult analysisResult = analyzeFile(file.getInputStream(), user.getUsername(), ipAddress);
        List<Course> rawCourses = analysisResult.getRawCourses();
        
        // 분석된 과목들을 학기별로 그룹화하고 정렬
        TranscriptParsingResult parsingResult = groupAndFormatCourses(rawCourses);

        // 사용자의 마지막 학기 정보 업데이트
        user.setLastSemester(parsingResult.getLastSemester());
        userRepository.save(user);

        // 분석된 수강 이력을 DB에 저장 (기존 이력은 삭제됨)
        saveCoursesToDatabase(user, parsingResult.getCoursesBySemester());
    }

    /**
     * 성적표 파일(CSV/Text)을 줄 단위로 읽어 과목 정보를 추출합니다.
     * 정규표현식을 사용하여 학기, 학수번호, 과목명, 학점 등을 파싱합니다.
     */
    private FileAnalysisResult analyzeFile(InputStream inputStream, String userId, String ipAddress) throws IOException {
        List<Course> rawCourses = new ArrayList<>();
        Map<String, String> majorInfo = new HashMap<>();

        // 성적표 라인 파싱을 위한 정규식
        // 예: "2023-1  CSE2010  자료구조  3.0  A+"
        Pattern coursePattern = Pattern.compile("^(20\\d{2}-[12SW])\\s*([A-Z]{3,4}\\d{3,4})\\s*(.+?)\\s*([0-9]+\\.[0-9])\\s*([A-F][+-]?|S|U|P|F|W)?.*$");
        Pattern majorPattern = Pattern.compile("1전공(.+?)2전공(.+?)3전공(.+)");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "EUC-KR"))) {
            String line;
            boolean majorInfoParsed = false;

            while ((line = reader.readLine()) != null) {
                // 전공 정보 파싱 (파일 상단에 위치한다고 가정)
                if (!majorInfoParsed) {
                    Matcher majorMatcher = majorPattern.matcher(line);
                    if (majorMatcher.find()) {
                        majorInfo.put("major1", majorMatcher.group(1));
                        majorInfo.put("major2", majorMatcher.group(2));
                        majorInfo.put("major3", majorMatcher.group(3));
                        majorInfoParsed = true;
                        continue;
                    }
                }

                // 과목 정보 파싱
                Matcher courseMatcher = coursePattern.matcher(line);
                if (courseMatcher.find()) {
                    String rawSemester = courseMatcher.group(1);
                    String courseCode = courseMatcher.group(2);
                    String courseName = courseMatcher.group(3).trim();

                    // 새로운 과목 발견 시 CourseMapping에 추가 (기본 학기 4로 설정)
                    if (!courseMappingRepository.existsById(courseCode)) {
                        CourseMapping newMapping = new CourseMapping();
                        newMapping.setCourseCode(courseCode);
                        newMapping.setCourseName(courseName);
                        newMapping.setSemester(4); 

                        courseMappingRepository.save(newMapping);
                        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                        System.out.println(String.format("NewCourse: %s, User:%s, IP:%s, CODE: %s, SEMESTER: 4", timestamp, userId, ipAddress, courseCode));
                    }

                    rawCourses.add(new Course(rawSemester, courseCode, courseName, ""));
                }
            }
        }

        return new FileAnalysisResult(rawCourses, majorInfo);
    }

    /**
     * 추출된 과목 리스트를 학기별로 그룹화하고, 시간 순서대로 정렬합니다.
     * 계절학기 처리 로직도 포함되어 있습니다.
     */
    public TranscriptParsingResult groupAndFormatCourses(List<Course> rawCourses) {
        Map<String, Course> earliestCourseInstances = new HashMap<>();
        // 학기 순으로 정렬
        rawCourses.sort(Comparator.comparing(course -> new SemesterInfo(course.getSemester())));

        // 재수강 등으로 중복된 과목이 있을 경우, 가장 먼저 수강한 기록만 남김 (선택 사항)
        for (Course course : rawCourses) {
            String courseCode = course.getCourseCode();
            if (!earliestCourseInstances.containsKey(courseCode)) {
                earliestCourseInstances.put(courseCode, course);
            }
        }

        List<Course> processedCourses = new ArrayList<>(earliestCourseInstances.values());
        processedCourses.sort(Comparator.comparing(course -> new SemesterInfo(course.getSemester())));

        Map<String, List<Course>> coursesByFormattedSemester = new LinkedHashMap<>();
        Map<String, String> semesterIdentifierToFormattedKeyMap = new HashMap<>();
        int continuousSemesterCounter = 0;
        int lastRegularSemesterNumber = 0;

        for (Course course : processedCourses) {
            SemesterInfo currentSemesterInfo = new SemesterInfo(course.getSemester());
            String semesterIdentifier = currentSemesterInfo.toString();
            String formattedKey;

            if (semesterIdentifierToFormattedKeyMap.containsKey(semesterIdentifier)) {
                formattedKey = semesterIdentifierToFormattedKeyMap.get(semesterIdentifier);
            } else {
                // 정규 학기 카운팅 (휴학 제외, 실제 수강 학기 기준)
                if (currentSemesterInfo.isRegularSemester()) {
                    continuousSemesterCounter++;
                    lastRegularSemesterNumber = continuousSemesterCounter;
                    String semesterTypeDisplay = currentSemesterInfo.getType().equals("1") ? "1학기" : "2학기";
                    formattedKey = String.format("%d학기 (%s년 %s)", continuousSemesterCounter, currentSemesterInfo.getYear(), semesterTypeDisplay);
                } else {
                    // 계절 학기는 직전 정규 학기 + 0.5로 표시
                    double seasonalSemesterNumber = lastRegularSemesterNumber + 0.5;
                    String semesterTypeDisplay = currentSemesterInfo.getType().equals("S") ? "여름학기" : "겨울학기";
                    formattedKey = String.format("%.1f학기 (%s년 %s)", seasonalSemesterNumber, currentSemesterInfo.getYear(), semesterTypeDisplay);
                }
                semesterIdentifierToFormattedKeyMap.put(semesterIdentifier, formattedKey);
            }

            coursesByFormattedSemester.computeIfAbsent(formattedKey, k -> new ArrayList<>()).add(course);
        }

        // 각 학기 내에서 과목 코드로 정렬
        coursesByFormattedSemester.forEach((semester, courseList) -> courseList.sort(Comparator.comparing(Course::getCourseCode)));

        String lastSemesterString = processedCourses.isEmpty() ? null : processedCourses.get(processedCourses.size() - 1).getSemester();

        return new TranscriptParsingResult(coursesByFormattedSemester, lastSemesterString);
    }

    /**
     * 분석된 수강 이력을 데이터베이스에 저장합니다.
     * 해당 사용자의 기존 수강 이력은 모두 삭제 후 재생성됩니다.
     */
    @Transactional
    public void saveCoursesToDatabase(User user, Map<String, List<Course>> coursesBySemester) {
        semesterCourseRepository.deleteByUser(user);

        coursesBySemester.forEach((semesterString, courses) -> {
            double semesterNumber;
            // "1학기", "1.5학기" 등에서 숫자 추출
            Pattern pattern = Pattern.compile("^^([\\d\\.]+)\\학기");
            Matcher matcher = pattern.matcher(semesterString);
            if (matcher.find()) {
                semesterNumber = Double.parseDouble(matcher.group(1));
            } else {
                semesterNumber = 0.0;
            }

            courses.forEach(course -> {
                SemesterCourse sc = new SemesterCourse();
                sc.setUser(user);
                sc.setSemester(semesterNumber);
                sc.setCourseCode(course.getCourseCode());
                sc.setGrade(course.getRemark());
                semesterCourseRepository.save(sc);
            });
        });
    }

    /**
     * 파일에서 전공 정보만 빠르게 추출합니다. (미리보기 용도)
     */
    public List<String> extractMajorsFromFile(InputStream inputStream) throws IOException {
        List<String> majors = new ArrayList<>();
        Pattern majorPattern = Pattern.compile("1전공(.+?)2전공(.+?)3전공(.+)");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "EUC-KR"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher majorMatcher = majorPattern.matcher(line);
                if (majorMatcher.find()) {
                    majors.add(majorMatcher.group(1).trim());
                    majors.add(majorMatcher.group(2).trim());
                    majors.add(majorMatcher.group(3).trim());
                    break;
                }
            }
        }
        return majors;
    }

    public List<CourseMapping> getAllCourses() {
        return courseMappingRepository.findAll();
    }

    /**
     * 특정 과목의 학기별 수강생 수 통계를 반환합니다.
     * 1학기부터 8학기까지의 분포를 계산합니다.
     */
    public Map<String, Object> getCourseStats(String subjectCode) {
        List<SemesterCourse> courses = semesterCourseRepository.findByCourseCode(subjectCode);
        Map<Double, Long> semesterCounts = courses.stream().collect(Collectors.groupingBy(SemesterCourse::getSemester, Collectors.counting()));
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
        List<CourseMapping> courses = courseMappingRepository.findByCourseCodeStartingWithAndSemesterIn(majorPrefix, targetSemesters);
        return courses.stream()
                .map(course -> new CourseStatDto(course.getCourseCode(), course.getCourseName(), semesterCourseRepository.countDistinctUsersByCourseCode(course.getCourseCode())))
                .sorted(Comparator.comparingLong(CourseStatDto::getTotalStudentCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 전공이 아닌 과목(비전공/교양)을 조회합니다.
     */
    public List<CourseMapping> getNonMajorCourses(User user, Integer semester) {
        List<Integer> targetSemesters = new ArrayList<>(List.of(3, 4));
        if (semester != null) {
            targetSemesters.add(semester);
        } else {
            targetSemesters.addAll(List.of(1, 2));
        }
        List<CourseMapping> allCoursesInSemester = courseMappingRepository.findAllBySemesterIn(targetSemesters);
        
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
        OptionalDouble maxSemester = semesterCourseRepository.findByUser(user).stream()
                .mapToDouble(SemesterCourse::getSemester)
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
        List<CourseMapping> allCourses = courseMappingRepository.findBySemesterIn(targetSemesters);
        List<String> userTakenCourseCodes = semesterCourseRepository.findByUser(user).stream()
                .map(SemesterCourse::getCourseCode)
                .collect(Collectors.toList());

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
                        List<SemesterCourse> allTakes = semesterCourseRepository.findByCourseCode(course.getCourseCode());
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
                    .collect(Collectors.toList());

            geRecommendations = allCourses.stream()
                    .filter(course -> codesForUncompletedTracks.contains(course.getCourseCode()))
                    .filter(course -> !userTakenCourseCodes.contains(course.getCourseCode()))
                    .filter(course -> !cartCourseCodes.contains(course.getCourseCode()))
                    .filter(course -> !dismissedCourseCodes.contains(course.getCourseCode()))
                    .map(course -> {
                        String trackName = COURSE_CODE_TO_TRACK_NAME_MAP.get(course.getCourseCode());
                        int studentCount = semesterCourseRepository.findByCourseCode(course.getCourseCode()).size();
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
                        List<SemesterCourse> allTakes;
                        if (useMajorFilteredRecommendations) {
                            allTakes = semesterCourseRepository.findByCourseCodeAndUserMajor1(course.getCourseCode(), user.getMajor1());
                        } else {
                            allTakes = semesterCourseRepository.findByCourseCode(course.getCourseCode());
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
        List<String> userTakenCourseCodes = semesterCourseRepository.findByUser(user).stream()
                .map(SemesterCourse::getCourseCode)
                .collect(Collectors.toList());

        for (Map.Entry<Integer, List<String>> entry : GE_TRACKS.entrySet()) {
            boolean completed = userTakenCourseCodes.stream()
                    .anyMatch(takenCode -> entry.getValue().contains(takenCode));
            if (!completed) {
                return true; // Found an uncompleted track
            }
        }
        return false; // All tracks completed
    }
}
