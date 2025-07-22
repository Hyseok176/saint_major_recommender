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
        Map<String, String> semesterInfoToFormattedKeyMap = new HashMap<>(); // SemesterInfo -> "X학기 (YYYY년 X학기)"
        int continuousSemesterCounter = 0;
        SemesterInfo lastRegularSemesterInfo = null; // To track the last 1 or 2 semester

        for (Course course : processedCourses) {
            SemesterInfo currentSemesterInfo = new SemesterInfo(course.getSemester());
            SemesterInfo targetSemesterInfoForGrouping;
            String remark = "";

            if (currentSemesterInfo.isRegularSemester()) {
                targetSemesterInfoForGrouping = currentSemesterInfo;
                lastRegularSemesterInfo = currentSemesterInfo; // Update last regular semester
            } else {
                if (lastRegularSemesterInfo == null) {
                    targetSemesterInfoForGrouping = currentSemesterInfo;
                } else {
                    targetSemesterInfoForGrouping = lastRegularSemesterInfo;
                }
                remark = currentSemesterInfo.getType().equals("S") ? "여름학기 수강" : "겨울학기 수강";
                course.setRemark(remark);
            }

            String formattedKey;
            if (!semesterInfoToFormattedKeyMap.containsKey(targetSemesterInfoForGrouping.toString())) {
                continuousSemesterCounter++;
                String semesterTypeDisplayInParentheses;
                String targetType = targetSemesterInfoForGrouping.getType();
                switch (targetType) {
                    case "1": semesterTypeDisplayInParentheses = "1학기"; break;
                    case "2": semesterTypeDisplayInParentheses = "2학기"; break;
                    case "S": semesterTypeDisplayInParentheses = "여름학기"; break;
                    case "W": semesterTypeDisplayInParentheses = "겨울학기"; break;
                    default: semesterTypeDisplayInParentheses = targetType + "학기";
                }

                formattedKey = String.format("%d학기 (%s년 %s)",
                                             continuousSemesterCounter,
                                             targetSemesterInfoForGrouping.getYear(),
                                             semesterTypeDisplayInParentheses);
                semesterInfoToFormattedKeyMap.put(targetSemesterInfoForGrouping.toString(), formattedKey);
            } else {
                formattedKey = semesterInfoToFormattedKeyMap.get(targetSemesterInfoForGrouping.toString());
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
            // Extract the integer semester from the semesterString
            int semesterNumber;
            Pattern pattern = Pattern.compile("^(\\d+)학기");
            Matcher matcher = pattern.matcher(semesterString);
            if (matcher.find()) {
                semesterNumber = Integer.parseInt(matcher.group(1));
            } else {
                // Handle cases where the pattern doesn't match, perhaps log an error or default to 0
                semesterNumber = 0; // Or throw an exception, depending on desired error handling
            }

            courses.forEach(course -> {
                SemesterCourse sc = new SemesterCourse();
                sc.setUser(user);
                sc.setSemester(semesterNumber); // Use the extracted integer
                sc.setCourseName(course.getCourseName());
                sc.setGrade(course.getRemark());
                semesterCourseRepository.save(sc);
            });
        });
    }
}