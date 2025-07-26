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
    public List<Course> analyzeFile(InputStream inputStream) throws IOException {
        List<Course> rawCourses = new ArrayList<>();
        // Pattern to match lines containing course information (e.g., 2021-1 COR1015 스마트인간과사회)
        // Captures year, semester type, course code, and course name
        Pattern coursePattern = Pattern.compile("^(20\\d{2}-[12SW])\\t([A-Z]{3}\\d{4})\\t(.+?)\\t.*$");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("CP949")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // No more skipping sections, process all lines matching the pattern
                Matcher matcher = coursePattern.matcher(line);
                if (matcher.find()) {
                    String rawSemester = matcher.group(1);
                    String courseCode = matcher.group(2);
                    String courseName = matcher.group(3);

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
        return rawCourses;
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
        // This is a placeholder for the actual recommendation logic.
        // It currently returns a hardcoded list of courses.
        Map<String, List<Course>> recommendedCourses = new LinkedHashMap<>();

        List<Course> priority1 = new ArrayList<>();
        priority1.add(new Course("", "", "계산수학", "")); // Use courseName
        priority1.add(new Course("", "", "푸리에", "")); // Use courseName
        priority1.add(new Course("", "", "알바트로스세미나", "")); // Use courseName

        List<Course> priority2 = new ArrayList<>();
        priority2.add(new Course("", "", "선형대수학", "여름학기 수강")); // Use courseName

        List<Course> priority3 = new ArrayList<>();
        priority3.add(new Course("", "", "영어글로벌의사소통I", "")); // Use courseName
        priority3.add(new Course("", "", "자연계글쓰기", "")); // Use courseName

        recommendedCourses.put("1순위 추천과목 (7학기 빈도수 가장 높은 과목)", priority1);
        recommendedCourses.put("2순위 추천과목 (7학기 빈도수가 2위인 과목)", priority2);
        recommendedCourses.put("3순위 추천과목 (6,8 학기에 들은 과목)", priority3);

        return recommendedCourses;
    }
}