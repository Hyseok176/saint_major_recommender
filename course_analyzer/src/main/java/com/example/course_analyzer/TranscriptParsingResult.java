package com.example.course_analyzer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class TranscriptParsingResult {
    private final Map<String, List<Course>> coursesBySemester;
    private final String lastSemester;
}
