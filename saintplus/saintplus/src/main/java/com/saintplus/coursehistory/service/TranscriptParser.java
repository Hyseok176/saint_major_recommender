package com.saintplus.coursehistory.service;

import com.saintplus.coursehistory.domain.Remarks;
import com.saintplus.coursehistory.dto.CourseAnalysisData;
import com.saintplus.coursehistory.dto.SemesterInfo;
import com.saintplus.coursehistory.dto.TranscriptParsingResult;
import com.saintplus.coursehistory.dto.TranscriptScanResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptParser {

    private static final Pattern COURSE_PATTERN = Pattern.compile("^(20\\d{2}-[12SW])\\s*([A-Z]{3,4}\\d{3,4})\\s*(.+?)\\s*([0-9]+\\.[0-9])\\s*([A-F][+-]?|S|U|P|F|W)?\\s*(.*)$");
    private static final Pattern MAJOR_PATTERN = Pattern.compile("1전공(.+?)2전공(.+?)3전공(.+)");


    public TranscriptScanResult analyzeFile(InputStream inputStream, String userId) throws IOException {
        List<CourseAnalysisData> rawCourses = new ArrayList<>();
        Map<String, String> mappingCourseCodeName = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "EUC-KR"))) {
            String line;

            while ((line = reader.readLine()) != null) {

                Matcher courseMatcher = COURSE_PATTERN.matcher(line);
                if (courseMatcher.find()) {
                    String rawSemester = courseMatcher.group(1);
                    String courseCode = courseMatcher.group(2);
                    String courseName = courseMatcher.group(3).trim();

                    String grade = courseMatcher.group(5).trim();
                    String rawRemarksString = courseMatcher.group(6).trim();
                    Remarks importantRemarks = processRemarks(grade, rawRemarksString);

                    rawCourses.add(CourseAnalysisData.builder()
                            .semester(rawSemester)
                            .courseCode(courseCode)
                            .courseName(courseName)
                            .importantRemarks(importantRemarks)
                            .build());

                    mappingCourseCodeName.putIfAbsent(courseCode, courseName);
                }
            }
        }

        log.info("File analysis complete. Found {} raw courses for user: {}", rawCourses.size(), userId);
        return new TranscriptScanResult(rawCourses, mappingCourseCodeName);
    }



    public Remarks processRemarks(String grade, String rawRemarksString){

        Remarks importantRemarks = new Remarks();

        switch (grade) {
            case "FA":  //결석허용초과 과목낙제
            case "F":   //과목미이수
            case "U":   //불합격
                importantRemarks.setFailed(true);
        }

        if (rawRemarksString == null || rawRemarksString.isBlank()) {
            return importantRemarks;
        }

        List<String> remarks = Arrays.stream(rawRemarksString.split(","))
                .map(String::trim)
                .filter(s->!s.isEmpty())
                .toList();

        for (String remark : remarks){
            switch (remark) {
                case "R":   //재이수로성적취득 후 기존성적 대체
                    importantRemarks.setRetake(true);
                    break;
                case "E":   //영어강의
                    importantRemarks.setEnglishLecture(true);
                    break;
                case "M":   //중복인정과목
                    importantRemarks.setDuplicate(true);
                    break;
            }
        }

        return importantRemarks;

    }



    public TranscriptParsingResult groupAndFormatCourses(List<CourseAnalysisData> rawCourses) {
        Map<String, CourseAnalysisData> earliestCourseInstances = new HashMap<>();
        rawCourses.sort(Comparator.comparing(course -> new SemesterInfo(course.getSemester())));

        for (CourseAnalysisData course : rawCourses) {
            String courseCode = course.getCourseCode();
            if (!earliestCourseInstances.containsKey(courseCode)) {
                earliestCourseInstances.put(courseCode, course);
            }
        }

        List<CourseAnalysisData> processedCourses = new ArrayList<>(earliestCourseInstances.values());
        processedCourses.sort(Comparator.comparing(course -> new SemesterInfo(course.getSemester())));

        Map<String, List<CourseAnalysisData>> coursesByFormattedSemester = new LinkedHashMap<>();
        Map<String, String> semesterIdentifierToFormattedKeyMap = new HashMap<>();
        int continuousSemesterCounter = 0;
        int lastRegularSemesterNumber = 0;

        for (CourseAnalysisData course : processedCourses) {
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

        coursesByFormattedSemester.forEach((semester, courseList) -> courseList.sort(Comparator.comparing(CourseAnalysisData::getCourseCode)));

        String lastSemesterString = processedCourses.isEmpty() ? null : processedCourses.get(processedCourses.size() - 1).getSemester();

        return new TranscriptParsingResult(coursesByFormattedSemester, lastSemesterString);
    }



    public List<String> extractMajorsFromFile(InputStream inputStream) throws IOException {
        List<String> majors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "EUC-KR"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher majorMatcher = MAJOR_PATTERN.matcher(line);
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


}
