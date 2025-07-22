package com.example.course_analyzer;

public class Course {
    private String semester;
    private String courseCode;
    private String courseName;
    private String remark; // New field for remarks

    public Course(String semester, String courseCode, String courseName, String remark) {
        this.semester = semester;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.remark = remark;
    }

    // Constructor without remark for regular semesters
    public Course(String semester, String courseCode, String courseName) {
        this(semester, courseCode, courseName, "");
    }

    public Course(String courseName, String grade) {
        this.courseName = courseName;
        this.remark = grade; // grade를 remark 필드에 저장
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
