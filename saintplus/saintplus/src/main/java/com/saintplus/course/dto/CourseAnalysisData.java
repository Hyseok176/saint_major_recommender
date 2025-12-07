package com.saintplus.course.dto;

import com.saintplus.transcript.domain.Remarks;
import lombok.*;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CourseAnalysisData {

    private String semester;
    private String courseCode;
    private String courseName;

    private String courseStatus; // New field for remarks - 실제수강과목, 장바구니과목 분류

    private double percentage; // New field for percentage
    private long totalStudents; // New field for total students who took the course
    private long countInMostFrequentSemester; // New field for count of students in most frequent semester

    private Remarks importantRemarks; // 임시 필드 - 학부이수표 파싱 단계에서 비고 저장

    // Constructor without remark for regular semesters
    public CourseAnalysisData(String semester, String courseCode, String courseName) {
        this(semester, courseCode, courseName, "", 0.0, 0L, 0L, null);
    }

    public CourseAnalysisData(String semester, String courseCode, String courseName, String courseStatus) {
        this(semester, courseCode, courseName, courseStatus, 0.0, 0L, 0L, null);
    }

}
