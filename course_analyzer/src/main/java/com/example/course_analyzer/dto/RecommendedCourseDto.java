package com.example.course_analyzer.dto;

import com.example.course_analyzer.domain.CourseMapping;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendedCourseDto {
    private CourseMapping course;
    private double score;
    private int studentCount;
    private double averageProximityScore;
    private String trackName;
    private String majorName;
}
