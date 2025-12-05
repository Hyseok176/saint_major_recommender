package com.saintplus.coursehistory.dto;

import com.saintplus.coursehistory.domain.Remarks;
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
    private Remarks importantRemarks; // New field for remarks
    private double percentage; // New field for percentage
    private long totalStudents; // New field for total students who took the course
    private long countInMostFrequentSemester; // New field for count of students in most frequent semester


}
