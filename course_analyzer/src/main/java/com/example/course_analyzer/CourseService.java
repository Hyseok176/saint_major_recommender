package com.example.course_analyzer;

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

@Service
public class CourseService {

    private static final Map<Integer, List<String>> GE_TRACKS;
    private static final Map<String, String> COURSE_CODE_TO_TRACK_NAME_MAP;

    static {
        Map<Integer, List<String>> tracks = new HashMap<>();
        tracks.put(1, List.of("HFS2001", "HFS2002", "HFS2003", "HFU4012", "HFU4023"));
        tracks.put(2, List.of("ETS2001", "ETS2002", "ETS2004", "CHS2002", "CHS2003", "CHS2004", "HSS3032"));
        tracks.put(3, List.of("SHS2001", "SHS2002", "SHS2003", "SHS2007", "SHS2005"));
        tracks.put(4, List.of("STS2001", "STS2002", "STU4011", "STS2011", "STS2012", "STS2010", "STS2005", "STS2015"));
        GE_TRACKS = Collections.unmodifiableMap(tracks);

        Map<Integer, String> trackNames = Map.of(
            1, "인간과 신앙",
            2, "인간과 사상",
            3, "인간과 사회",
            4, "인간과 과학&AI"
        );

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

    @Transactional
    public void updateUserTranscript(User user, MultipartFile file, String major1, String major2, String major3, String ipAddress) throws IOException {
        user.setMajor1(major1.replace(" ", ""));
        user.setMajor2(major2.replace(" ", ""));
        user.setMajor3(major3.replace(" ", ""));

        FileAnalysisResult analysisResult = analyzeFile(file.getInputStream(), user.getUsername(), ipAddress);
        List<Course> rawCourses = analysisResult.getRawCourses();
        Map<String, List<Course>> coursesBySemester = groupAndFormatCourses(rawCourses);

        saveCoursesToDatabase(user, coursesBySemester);
    }

    private FileAnalysisResult analyzeFile(InputStream inputStream, String userId, String ipAddress) throws IOException {
        List<Course> rawCourses = new ArrayList<>();
        Map<String, String> majorInfo = new HashMap<>();

        Pattern coursePattern = Pattern.compile("^(20\\d{2}-[12SW])\\s*([A-Z]{3,4}\\d{3,4})\\s*(.+?)\\s*([0-9]+\\.[0-9])\\s*([A-F][+-]?|S|U|P|F|W)?.*$");
        Pattern majorPattern = Pattern.compile("1전공(.+?)2전공(.+?)3전공(.+)");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "EUC-KR"))) {
            String line;
            boolean majorInfoParsed = false;

            while ((line = reader.readLine()) != null) {
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

                Matcher courseMatcher = coursePattern.matcher(line);
                if (courseMatcher.find()) {
                    String rawSemester = courseMatcher.group(1);
                    String courseCode = courseMatcher.group(2);
                    String courseName = courseMatcher.group(3).trim();

                    if (!courseMappingRepository.existsById(courseCode)) {
                        CourseMapping newMapping = new CourseMapping();
                        newMapping.setCourseCode(courseCode);
                        newMapping.setCourseName(courseName);
                        newMapping.setSemester(4); // Set semester to 4 for all new courses

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

    public Map<String, List<Course>> groupAndFormatCourses(List<Course> rawCourses) {
        Map<String, Course> earliestCourseInstances = new HashMap<>();
        rawCourses.sort(Comparator.comparing(course -> new SemesterInfo(course.getSemester())));

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
                if (currentSemesterInfo.isRegularSemester()) {
                    continuousSemesterCounter++;
                    lastRegularSemesterNumber = continuousSemesterCounter;
                    String semesterTypeDisplay = currentSemesterInfo.getType().equals("1") ? "1학기" : "2학기";
                    formattedKey = String.format("%d학기 (%s년 %s)", continuousSemesterCounter, currentSemesterInfo.getYear(), semesterTypeDisplay);
                } else {
                    double seasonalSemesterNumber = lastRegularSemesterNumber + 0.5;
                    String semesterTypeDisplay = currentSemesterInfo.getType().equals("S") ? "여름학기" : "겨울학기";
                    formattedKey = String.format("%.1f학기 (%s년 %s)", seasonalSemesterNumber, currentSemesterInfo.getYear(), semesterTypeDisplay);
                }
                semesterIdentifierToFormattedKeyMap.put(semesterIdentifier, formattedKey);
            }

            coursesByFormattedSemester.computeIfAbsent(formattedKey, k -> new ArrayList<>()).add(course);
        }

        coursesByFormattedSemester.forEach((semester, courseList) -> courseList.sort(Comparator.comparing(Course::getCourseCode)));

        return coursesByFormattedSemester;
    }

    @Transactional
    public void saveCoursesToDatabase(User user, Map<String, List<Course>> coursesBySemester) {
        semesterCourseRepository.deleteByUser(user);

        coursesBySemester.forEach((semesterString, courses) -> {
            double semesterNumber;
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

    public Map<String, Object> getCourseStats(String subjectCode) {
        List<SemesterCourse> courses = semesterCourseRepository.findByCourseCode(subjectCode);
        Map<Double, Long> semesterCounts = courses.stream().collect(Collectors.groupingBy(SemesterCourse::getSemester, Collectors.counting()));
        Map<Double, Long> allSemesters = new LinkedHashMap<>();
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

    public List<CourseStatDto> getCoursesByMajor(String majorPrefix) {
        List<CourseMapping> courses = courseMappingRepository.findByCourseCodeStartingWith(majorPrefix);
        return courses.stream().map(course -> new CourseStatDto(course.getCourseCode(), course.getCourseName(), semesterCourseRepository.countDistinctUsersByCourseCode(course.getCourseCode()))).sorted(Comparator.comparingLong(CourseStatDto::getTotalStudentCount).reversed()).collect(Collectors.toList());
    }

    public List<CourseMapping> getNonMajorCourses(User user) {
        List<CourseMapping> allCourses = courseMappingRepository.findAll();
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

        // Filter out courses that belong to ANY of the user's declared majors
        return allCourses.stream()
                .filter(course -> userMajorPrefixes.stream().noneMatch(prefix -> course.getCourseCode().startsWith(prefix)))
                .collect(Collectors.toList());
    }

    private int getCurrentSemester(User user) {
        OptionalDouble maxSemester = semesterCourseRepository.findByUser(user).stream()
                .mapToDouble(SemesterCourse::getSemester)
                .max();
        return (int) Math.ceil(maxSemester.orElse(1.0));
    }

    public Map<String, List<RecommendedCourseDto>> recommendCourses(User user, List<String> cartCourseCodes, List<String> dismissedCourseCodes) {
        int currentUserSemester = getCurrentSemester(user);

        // 현재 월을 기준으로 학기 결정 (1-6월: 1학기, 7-12월: 2학기)
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        List<Integer> targetSemesters = new ArrayList<>();
        if (currentMonth >= 7) { // 하반기 -> 2학기 또는 공통 과목 추천
            targetSemesters.add(2);
            targetSemesters.add(3);
        } else { // 상반기 -> 1학기 또는 공통 과목 추천
            targetSemesters.add(1);
            targetSemesters.add(3);
        }
        targetSemesters.add(4); // semester가 4인 과목은 항상 포함

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

        // Major Recommendations
        List<RecommendedCourseDto> majorRecommendations = new ArrayList<>();
        if (!userMajorPrefixes.isEmpty()) {
            // Create a map from prefix to major name for easy lookup
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
                            return new RecommendedCourseDto(course, 0.01, 0, 0, null, majorName);
                        }
                        List<SemesterCourse> allTakes = semesterCourseRepository.findByCourseCode(course.getCourseCode());
                        double score = allTakes.stream()
                                .mapToDouble(sc -> 1.0 / (1.0 + Math.abs(currentUserSemester - sc.getSemester())))
                                .sum();
                        double averageProximity = allTakes.isEmpty() ? 0 : score / allTakes.size();
                        return new RecommendedCourseDto(course, score, allTakes.size(), averageProximity, null, majorName);
                    })
                    .sorted(Comparator.comparingDouble(RecommendedCourseDto::getScore).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
        }

        // GE Recommendations
        List<RecommendedCourseDto> geRecommendations;

        // 1. Find uncompleted tracks using the new exact course codes
        List<Integer> uncompletedTracks = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : GE_TRACKS.entrySet()) {
            boolean completed = userTakenCourseCodes.stream()
                    .anyMatch(takenCode -> entry.getValue().contains(takenCode));
            if (!completed) {
                uncompletedTracks.add(entry.getKey());
            }
        }

        // 2. Generate recommendations based on track completion
        if (!uncompletedTracks.isEmpty()) {
            // Case 1: Recommend from uncompleted tracks
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
                        return new RecommendedCourseDto(course, 0, studentCount, 0, trackName);
                    })
                    .sorted(Comparator.comparingInt(RecommendedCourseDto::getStudentCount).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
        } else {
            // Case 2: All tracks completed, use original logic
            List<String> allMajorPrefixes = List.of("MAT", "PHY", "CHM", "BIO", "EEE", "MEE", "CSE", "CBE", "SSE", "AIE", "ECO", "MGT", "EDU");
            final boolean useMajorFilteredRecommendations = user.getMajor1() != null && !user.getMajor1().isEmpty() && !user.getMajor1().equals("미선택");

            geRecommendations = allCourses.stream()
                    .filter(course -> allMajorPrefixes.stream().noneMatch(prefix -> course.getCourseCode().startsWith(prefix)))
                    .filter(course -> !userTakenCourseCodes.contains(course.getCourseCode()))
                    .filter(course -> !cartCourseCodes.contains(course.getCourseCode()))
                    .filter(course -> !dismissedCourseCodes.contains(course.getCourseCode()))
                    .map(course -> {
                        if (course.getSemester() != null && course.getSemester() == 4) {
                            return new RecommendedCourseDto(course, 0.01, 0, 0);
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
                        return new RecommendedCourseDto(course, score, allTakes.size(), averageProximity);
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
    public SavedCourse addSavedCourse(String username, String courseCode, String courseName) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        if (savedCourseRepository.findByUser(user).size() >= 8) {
            throw new IllegalStateException("장바구니에는 최대 8과목까지 담을 수 있습니다.");
        }
        if (savedCourseRepository.existsByUserAndCourseCode(user, courseCode)) {
            throw new IllegalStateException("이미 장바구니에 담긴 과목입니다.");
        }
        SavedCourse savedCourse = new SavedCourse(user, courseCode, courseName);
        return savedCourseRepository.save(savedCourse);
    }

    @Transactional
    public void deleteSavedCourse(String username, String courseCode) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        savedCourseRepository.deleteByUserAndCourseCode(user, courseCode);
    }

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
