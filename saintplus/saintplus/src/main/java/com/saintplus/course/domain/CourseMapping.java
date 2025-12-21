package com.saintplus.course.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "course_mapping")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class CourseMapping {

    @Id
    private String courseCode;
    private String courseName;
    private Integer semester; // 1: 1학기, 2: 2학기, 3: 1,2학기

    public CourseMapping(String courseCode, String courseName) {
        this.courseCode = courseCode;
        this.courseName = courseName;
    }
}
