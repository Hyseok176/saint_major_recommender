package com.example.course_analyzer;

import lombok.Data;

@Data
public class RecommendedCourseDto {
    private CourseMapping course;
    private double score;
    private int studentCount;
    private double averageProximityScore;
    private String trackName;
    private String majorName; // 전공 이름 필드 추가

    // 모든 필드를 포함하는 생성자
    public RecommendedCourseDto(CourseMapping course, double score, int studentCount, double averageProximityScore, String trackName, String majorName) {
        this.course = course;
        this.score = score;
        this.studentCount = studentCount;
        this.averageProximityScore = averageProximityScore;
        this.trackName = trackName;
        this.majorName = majorName;
    }

    // 교양 과목 추천을 위한 생성자
    public RecommendedCourseDto(CourseMapping course, double score, int studentCount, double averageProximityScore, String trackName) {
        this(course, score, studentCount, averageProximityScore, trackName, null);
    }

    // 기존 코드를 위한 호환성 생성자
    public RecommendedCourseDto(CourseMapping course, double score, int studentCount, double averageProximityScore) {
        this(course, score, studentCount, averageProximityScore, null, null);
    }
}