package com.example.course_analyzer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 처리를 위해 추가
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
public class CourseService {

    @Autowired
    private SemesterCourseRepository semesterCourseRepository; // SemesterCourseRepository 주입

    @Autowired
    private CourseMappingRepository courseMappingRepository; // CourseMappingRepository 주입

    // analyzeFile now returns a raw list of courses, including re-taken ones
    public Map<String, Object> analyzeFile(InputStream inputStream) throws IOException {
        List<Course> rawCourses = new ArrayList<>();
        Map<String, String> majorInfo = new HashMap<>();

        // Pattern to match lines containing course information, accommodating different formats
        Pattern coursePattern = Pattern.compile("^(20\\d{2}-[12SW])([A-Z]+[0-9]+)(.+?)(\\d+\\.\\d+.*)?$");

        // Pattern to match major information (e.g., "1전공수학2전공경제3전공물리학")
        Pattern majorPattern = Pattern.compile("1전공(.+?)2전공(.+?)3전공(.+)");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            boolean majorInfoParsed = false;

            while ((line = reader.readLine()) != null) {
                // 전공 정보 파싱 (파일의 첫 부분에서 한 번만 시도)
                if (!majorInfoParsed) {
                    Matcher majorMatcher = majorPattern.matcher(line);
                    if (majorMatcher.find()) {
                        majorInfo.put("major1", majorMatcher.group(1));
                        majorInfo.put("major2", majorMatcher.group(2));
                        majorInfo.put("major3", majorMatcher.group(3));
                        majorInfoParsed = true;
                        // 전공 정보 라인은 과목 정보로 처리하지 않고 다음 라인으로 넘어감
                        continue;
                    }
                }

                // 과목 정보 파싱
                Matcher courseMatcher = coursePattern.matcher(line);
                if (courseMatcher.find()) {
                    String rawSemester = courseMatcher.group(1);
                    String courseCode = courseMatcher.group(2);
                    String courseName = courseMatcher.group(3).trim();

                    // 과목 코드와 과목 이름 매핑 저장
                    if (!courseMappingRepository.existsById(courseCode)) {
                        CourseMapping newMapping = new CourseMapping();
                        newMapping.setCourseCode(courseCode);
                        newMapping.setCourseName(courseName);
                        courseMappingRepository.save(newMapping);
                    }

                    // Remark will be set later in groupAndFormatCourses
                    rawCourses.add(new Course(rawSemester, courseCode, courseName, ""));
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("rawCourses", rawCourses);
        result.put("majorInfo", majorInfo);
        return result;
    }

    // New method to group and format courses by continuous semester
    public Map<String, List<Course>> groupAndFormatCourses(List<Course> rawCourses) {
        // Step 1: Initial Course Processing and Earliest Semester Tracking
        Map<String, Course> earliestCourseInstances = new HashMap<>(); // courseCode -> earliest Course instance

        // Sort rawCourses by semester to ensure we find the earliest instance correctly
        rawCourses.sort(Comparator.comparing(course -> new SemesterInfo(course.getSemester())));

        for (Course course : rawCourses) {
            String courseCode = course.getCourseCode();
            SemesterInfo currentSemesterInfo = new SemesterInfo(course.getSemester());

            if (!earliestCourseInstances.containsKey(courseCode)) {
                earliestCourseInstances.put(courseCode, course);
            } else {
                Course existingCourse = earliestCourseInstances.get(courseCode);
                SemesterInfo existingSemesterInfo = new SemesterInfo(existingCourse.getSemester());

                if (currentSemesterInfo.compareTo(existingSemesterInfo) < 0) {
                    earliestCourseInstances.put(courseCode, course); // Update with earlier instance
                }
            }
        }

        // Step 2: Prepare Courses for Grouping (from earliestCourseInstances)
        List<Course> processedCourses = new ArrayList<>(earliestCourseInstances.values());
        // Sort these processed courses chronologically by their earliest semester
        processedCourses.sort(Comparator.comparing(course -> new SemesterInfo(course.getSemester())));

        // Step 3: Grouping and Formatting
        Map<String, List<Course>> coursesByFormattedSemester = new LinkedHashMap<>();
        Map<String, String> semesterIdentifierToFormattedKeyMap = new HashMap<>(); // "2021-1" -> "1학기 (2021년 1학기)"
        int continuousSemesterCounter = 0;
        int lastRegularSemesterNumber = 0;

        for (Course course : processedCourses) {
            SemesterInfo currentSemesterInfo = new SemesterInfo(course.getSemester());
            String semesterIdentifier = currentSemesterInfo.toString();
            String formattedKey;

            if (semesterIdentifierToFormattedKeyMap.containsKey(semesterIdentifier)) {
                formattedKey = semesterIdentifierToFormattedKeyMap.get(semesterIdentifier);
            } else {
                String semesterTypeDisplay;
                if (currentSemesterInfo.isRegularSemester()) {
                    continuousSemesterCounter++;
                    lastRegularSemesterNumber = continuousSemesterCounter;
                    semesterTypeDisplay = currentSemesterInfo.getType().equals("1") ? "1학기" : "2학기";
                    formattedKey = String.format("%d학기 (%s년 %s)",
                            continuousSemesterCounter,
                            currentSemesterInfo.getYear(),
                            semesterTypeDisplay);
                } else { // Seasonal semester
                    double seasonalSemesterNumber = lastRegularSemesterNumber + 0.5;
                    String remark;
                    if (currentSemesterInfo.getType().equals("S")) {
                        semesterTypeDisplay = "여름학기";
                        remark = "여름학기 수강";
                    } else {
                        semesterTypeDisplay = "겨울학기";
                        remark = "겨울학기 수강";
                    }
                    course.setRemark(remark);
                    formattedKey = String.format("%.1f학기 (%s년 %s)",
                            seasonalSemesterNumber,
                            currentSemesterInfo.getYear(),
                            semesterTypeDisplay);
                }
                semesterIdentifierToFormattedKeyMap.put(semesterIdentifier, formattedKey);
            }

            if (!currentSemesterInfo.isRegularSemester()) {
                String remark = currentSemesterInfo.getType().equals("S") ? "여름학기 수강" : "겨울학기 수강";
                course.setRemark(remark);
            }

            coursesByFormattedSemester.computeIfAbsent(formattedKey, k -> new ArrayList<>()).add(course);
        }


        // Step 4: Final Sorting within Semesters by course code
        coursesByFormattedSemester.forEach((semester, courseList) -> {
            courseList.sort(Comparator.comparing(Course::getCourseCode));
        });

        return coursesByFormattedSemester;
    }

    @Transactional // 트랜잭션으로 묶어 데이터 일관성 유지
    public void saveCoursesToDatabase(User user, Map<String, List<Course>> coursesBySemester) {
        // 기존에 저장된 해당 유저의 학기별 교과목 정보 삭제 (새로운 파일 업로드 시 덮어쓰기)
        semesterCourseRepository.findByUser(user).forEach(semesterCourseRepository::delete);

        coursesBySemester.forEach((semesterString, courses) -> {
            // Extract the double semester from the semesterString
            double semesterNumber;
            Pattern pattern = Pattern.compile("^^([\\d\\.]+)학기");
            Matcher matcher = pattern.matcher(semesterString);
            if (matcher.find()) {
                semesterNumber = Double.parseDouble(matcher.group(1));
            } else {
                // Handle cases where the pattern doesn't match, perhaps log an error or default to 0
                semesterNumber = 0.0; // Or throw an exception, depending on desired error handling
            }

            courses.forEach(course -> {
                SemesterCourse sc = new SemesterCourse();
                sc.setUser(user);
                sc.setSemester(semesterNumber); // Use the extracted double
                sc.setCourseCode(course.getCourseCode()); // Use courseCode
                sc.setGrade(course.getRemark());
                semesterCourseRepository.save(sc);
            });
        });
    }

    public Map<String, List<Course>> recommendCourses(User user) {
        Map<String, List<Course>> recommendedCoursesBySemester = new LinkedHashMap<>();

        // 1. 모든 MAT 과목 조회
        List<CourseMapping> allMatCourses = courseMappingRepository.findByCourseCodeStartingWith("MAT");

        // 2. 사용자가 이미 이수한 과목 코드 조회
        List<String> userTakenCourseCodes = semesterCourseRepository.findByUser(user).stream()
                .map(SemesterCourse::getCourseCode)
                .collect(Collectors.toList());

        // 3. 미이수 MAT 과목 필터링
        List<CourseMapping> unTakenMatCourses = allMatCourses.stream()
                .filter(course -> !userTakenCourseCodes.contains(course.getCourseCode()))
                .collect(Collectors.toList());

        // 4. 각 미이수 MAT 과목에 대해 최빈 이수 학기 계산 및 그룹화
        for (CourseMapping matCourse : unTakenMatCourses) {
            String courseCode = matCourse.getCourseCode();
            String courseName = matCourse.getCourseName();

            // 해당 과목의 모든 이수 기록 조회
            List<SemesterCourse> allInstancesOfCourse = semesterCourseRepository.findByCourseCode(courseCode);

            // 학기별 빈도수 계산
            Map<Double, Long> semesterFrequency = allInstancesOfCourse.stream()
                    .collect(Collectors.groupingBy(SemesterCourse::getSemester, Collectors.counting()));

            if (!semesterFrequency.isEmpty()) {
                // 최빈 학기 찾기
                Map.Entry<Double, Long> mostFrequentEntry = semesterFrequency.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .get();
                double mostFrequentSemester = mostFrequentEntry.getKey();
                long countAtMostFrequentSemester = mostFrequentEntry.getValue();

                // 전체 학생 수 (해당 과목을 수강한 모든 학생 수)
                long totalStudentsForCourse = allInstancesOfCourse.stream()
                        .map(SemesterCourse::getUser) // User 객체로 매핑
                        .distinct() // 중복 사용자 제거
                        .count();

                double percentage = (totalStudentsForCourse > 0) ? (double) countAtMostFrequentSemester / totalStudentsForCourse * 100 : 0.0;

                // 추천 과목 리스트에 추가
                recommendedCoursesBySemester.computeIfAbsent(String.format("%d학기", (int) Math.floor(mostFrequentSemester)), k -> new ArrayList<>()).add(new Course("", courseCode, courseName, "", percentage, totalStudentsForCourse, countAtMostFrequentSemester));
            }
        }

        // 학기 순서대로 정렬 (LinkedHashMap이므로 키 순서 유지)
        return recommendedCoursesBySemester.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparingDouble(s -> {
                    // "X학기" 또는 "X.Y학기"에서 숫자 부분만 추출하여 double로 변환
                    Pattern pattern = Pattern.compile("([\\d\\.]+)학기");
                    Matcher matcher = pattern.matcher(s);
                    if (matcher.find()) {
                        return Double.parseDouble(matcher.group(1));
                    }
                    return 0.0; // 매칭되지 않으면 0.0으로 처리 (오류 방지)
                })))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, // Merge function, not relevant for sorted
                        LinkedHashMap::new
                ));
    }
}