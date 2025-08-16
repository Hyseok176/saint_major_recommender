package com.example.course_analyzer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecommendedCourseDto {
    private CourseMapping course;
    private double score;
    private int studentCount;
    private double averageProximityScore;
}
