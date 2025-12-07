package com.saintplus.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseStatDto {
    private String courseCode;
    private String courseName;
    private long totalStudentCount;
}
