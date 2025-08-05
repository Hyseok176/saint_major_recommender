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
    public Map<String, Object> analyzeFile(InputStream inputStream, String userId, String ipAddress) throws IOException {
        List<Course> rawCourses = new ArrayList<>();
        Map<String, String> majorInfo = new HashMap<>();

        // Pattern to match lines containing course information, allowing for various formats
        Pattern coursePattern = Pattern.compile("^(20\\d{2}-[12SW])\\s*([A-Z]{3,4}\\d{3,4})\\s*(.+?)\\s*([0-9]+\\.[0-9])\\s*([A-F][+-]?|S|U|P|F|W)?.*$");

        // Pattern to match major information (e.g., "1전공수학2전공경제3전공물리학")
        Pattern majorPattern = Pattern.compile("1전공(.+?)2전공(.+?)3전공(.+)");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "EUC-KR"))) {
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
                        courseMappingRepository.save(newMapping);
                        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                        System.out.println(String.format("NewCourse: %s, User:%s, IP:%s, CODE: %s", timestamp, userId, ipAddress, courseCode));
                    }

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

    public List<CourseMapping> getAllCourses() {
        return courseMappingRepository.findAllExcludingCodes();
    }

    public Map<String, Object> getCourseStats(String subjectCode) {
        List<SemesterCourse> courses = semesterCourseRepository.findByCourseCode(subjectCode);
        Map<Double, Long> semesterCounts = courses.stream()
                .collect(Collectors.groupingBy(SemesterCourse::getSemester, Collectors.counting()));

        // Initialize map with all regular semesters from 1 to 8
        Map<Double, Long> allSemesters = new LinkedHashMap<>();
        for (int i = 1; i <= 8; i++) {
            allSemesters.put((double) i, 0L);
        }

        // Populate with actual counts and add seasonal semesters if they exist
        semesterCounts.forEach((semester, count) -> {
            if (semester % 1 != 0) { // Seasonal semester
                allSemesters.put(semester, count);
            } else { // Regular semester
                allSemesters.put(semester, count);
            }
        });

        // Sort by semester
        Map<Double, Long> sortedSemesterCounts = new LinkedHashMap<>();
        allSemesters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(x -> sortedSemesterCounts.put(x.getKey(), x.getValue()));

        List<String> labels = sortedSemesterCounts.keySet().stream()
                .map(semester -> {
                    if (semester % 1 == 0) {
                        return String.format("%.0f학기", semester);
                    } else {
                        return String.format("%.1f학기", semester);
                    }
                })
                .collect(Collectors.toList());
        List<Long> values = new ArrayList<>(sortedSemesterCounts.values());

        Map<String, Object> stats = new HashMap<>();
        stats.put("labels", labels);
        stats.put("values", values);
        return stats;
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
                    break; // 전공 정보를 찾으면 더 이상 파일을 읽지 않음
                }
            }
        }
        return majors;
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

    public List<CourseMapping> getFilteredCoursesByMajor(List<String> majorPrefixes) {
        List<CourseMapping> filteredCourses = new ArrayList<>();
        for (String prefix : majorPrefixes) {
            filteredCourses.addAll(courseMappingRepository.findByCourseCodeStartingWith(prefix));
        }
        return filteredCourses.stream()
                .distinct() // Remove duplicates if a course matches multiple major prefixes
                .collect(Collectors.toList());
    }

    public Map<String, List<Course>> recommendCourses(User user) {
        Map<String, List<Course>> recommendedCoursesBySemester = new LinkedHashMap<>();

        // Get major prefixes for the user
        List<String> userMajorPrefixes = new ArrayList<>();
        if (user.getMajor1() != null && !user.getMajor1().equals("미선택")) {
            userMajorPrefixes.add(getCoursePrefixForMajor(user.getMajor1()));
        }
        if (user.getMajor2() != null && !user.getMajor2().equals("미선택")) {
            userMajorPrefixes.add(getCoursePrefixForMajor(user.getMajor2()));
        }
        if (user.getMajor3() != null && !user.getMajor3().equals("미선택")) {
            userMajorPrefixes.add(getCoursePrefixForMajor(user.getMajor3()));
        }

        if (userMajorPrefixes.isEmpty()) {
            return recommendedCoursesBySemester; // No major selected, return empty recommendations
        }

        // 1. Get all courses for the user's majors
        List<CourseMapping> allMajorCourses = new ArrayList<>();
        for (String prefix : userMajorPrefixes) {
            allMajorCourses.addAll(courseMappingRepository.findByCourseCodeStartingWith(prefix));
        }

        // 2. Get user's taken courses
        List<String> userTakenCourseCodes = semesterCourseRepository.findByUser(user).stream()
                .map(SemesterCourse::getCourseCode)
                .collect(Collectors.toList());

        // 3. Filter out courses already taken by the user
        List<CourseMapping> unTakenMajorCourses = allMajorCourses.stream()
                .filter(course -> !userTakenCourseCodes.contains(course.getCourseCode()))
                .collect(Collectors.toList());

        // 4. Calculate and group by most frequent semester for each untaken major course
        for (CourseMapping majorCourse : unTakenMajorCourses) {
            String courseCode = majorCourse.getCourseCode();
            String courseName = majorCourse.getCourseName();

            List<SemesterCourse> allInstancesOfCourse = semesterCourseRepository.findByCourseCode(courseCode);

            Map<Double, Long> semesterFrequency = allInstancesOfCourse.stream()
                    .collect(Collectors.groupingBy(SemesterCourse::getSemester, Collectors.counting()));

            if (!semesterFrequency.isEmpty()) {
                Map.Entry<Double, Long> mostFrequentEntry = semesterFrequency.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .get();
                double mostFrequentSemester = mostFrequentEntry.getKey();
                long countAtMostFrequentSemester = mostFrequentEntry.getValue();

                long totalStudentsForCourse = allInstancesOfCourse.stream()
                        .map(SemesterCourse::getUser)
                        .distinct()
                        .count();

                double percentage = (totalStudentsForCourse > 0) ? (double) countAtMostFrequentSemester / totalStudentsForCourse * 100 : 0.0;

                recommendedCoursesBySemester.computeIfAbsent(String.format("%d학기", (int) Math.floor(mostFrequentSemester)), k -> new ArrayList<>()).add(new Course("", courseCode, courseName, "", percentage, totalStudentsForCourse, countAtMostFrequentSemester));
            }
        }

        return recommendedCoursesBySemester.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparingDouble(s -> {
                    Pattern pattern = Pattern.compile("([\\d\\.]+)학기");
                    Matcher matcher = pattern.matcher(s);
                    if (matcher.find()) {
                        return Double.parseDouble(matcher.group(1));
                    }
                    return 0.0;
                })))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private String getCoursePrefixForMajor(String major) {
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
            default: return "";
        }
    }
}