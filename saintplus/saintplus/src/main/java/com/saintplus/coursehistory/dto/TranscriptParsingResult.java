package com.saintplus.coursehistory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;


@Getter
@AllArgsConstructor
public class TranscriptParsingResult {
    private final Map<String, List<CourseAnalysisData>> coursesBySemester;
    private final String lastSemester;
}
