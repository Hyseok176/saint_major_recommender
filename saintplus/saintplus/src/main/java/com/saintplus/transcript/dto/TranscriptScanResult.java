package com.saintplus.transcript.dto;

import com.saintplus.course.dto.CourseAnalysisData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;


@Getter
@AllArgsConstructor
public class TranscriptScanResult {
    private final List<CourseAnalysisData> rawCourses;
    private final Map<String,String> mappingCourseCodeName;
}
