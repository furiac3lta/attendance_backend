package com.marcedev.attendance.dto;

public class CourseMonthlyAttendanceDTO {

    private Long studentId;
    private String studentName;
    private Long present;
    private Double totalClasses; // <── CAMBIÓ A DOUBLE
    private Double percent;

    public CourseMonthlyAttendanceDTO(Long studentId, String studentName, Long present, Double totalClasses, Double percent) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.present = present;
        this.totalClasses = totalClasses;
        this.percent = percent;
    }

    public Long getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public Long getPresent() { return present; }
    public Double getTotalClasses() { return totalClasses; }
    public Double getPercent() { return percent; }
}
