package com.example.course_analyzer;

public class Course {
    private String semester;
    private String courseCode;
    private String courseName;
    private String remark; // New field for remarks
    private double percentage; // New field for percentage
    private long totalStudents; // New field for total students who took the course
    private long countInMostFrequentSemester; // New field for count of students in most frequent semester

    public Course(String semester, String courseCode, String courseName, String remark, double percentage, long totalStudents, long countInMostFrequentSemester) {
        this.semester = semester;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.remark = remark;
        this.percentage = percentage;
        this.totalStudents = totalStudents;
        this.countInMostFrequentSemester = countInMostFrequentSemester;
    }

    public Course(String semester, String courseCode, String courseName, String remark, double percentage) {
        this(semester, courseCode, courseName, remark, percentage, 0L, 0L); // Default new fields to 0
    }

    // Constructor without remark for regular semesters
    public Course(String semester, String courseCode, String courseName) {
        this(semester, courseCode, courseName, "", 0.0, 0L, 0L);
    }

    public Course(String semester, String courseCode, String courseName, String remark) {
        this(semester, courseCode, courseName, remark, 0.0, 0L, 0L);
    }

    public Course(String courseName, String grade) {
        this("", "", courseName, grade, 0.0, 0L, 0L); // Adjust this constructor to use the new main constructor
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

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getCountInMostFrequentSemester() {
        return countInMostFrequentSemester;
    }

    public void setCountInMostFrequentSemester(long countInMostFrequentSemester) {
        this.countInMostFrequentSemester = countInMostFrequentSemester;
    }
}
