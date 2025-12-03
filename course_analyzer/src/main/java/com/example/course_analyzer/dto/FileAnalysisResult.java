package com.example.course_analyzer.dto;

import com.example.course_analyzer.domain.Course;

import java.util.List;
import java.util.Map;

public class FileAnalysisResult {
    private final List<Course> rawCourses;
    private final Map<String, String> majorInfo;

    public FileAnalysisResult(List<Course> rawCourses, Map<String, String> majorInfo) {
        this.rawCourses = rawCourses;
        this.majorInfo = majorInfo;
    }

    public List<Course> getRawCourses() {
        return rawCourses;
    }

    public Map<String, String> getMajorInfo() {
        return majorInfo;
    }
}
