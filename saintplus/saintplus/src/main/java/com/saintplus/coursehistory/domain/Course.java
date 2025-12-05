package com.saintplus.coursehistory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Entity
@Table(name = "COURSE")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Course {

    @Id
    private String courseCode;
    private String courseName;
    private Integer semester; // 1: 1학기, 2: 2학기, 3: 1,2학기

    public Course(String courseCode, String courseName) {
        this.courseCode = courseCode;
        this.courseName = courseName;
    }

}