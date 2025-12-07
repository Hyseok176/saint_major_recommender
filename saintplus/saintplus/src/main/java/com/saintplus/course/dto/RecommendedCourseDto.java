package com.saintplus.course.dto;

import com.saintplus.course.domain.Course;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendedCourseDto {
    private Course course;
    private double score;
    private int studentCount;
    private double averageProximityScore;
    private String trackName;
    private String majorName;
}
