package com.marcedev.attendance.dto;

public class CourseMonthlyAttendanceDTO {

    private Long studentId;
    private String studentName;
    private Long present;
    private Long totalClasses; // <── CAMBIÓ A DOUBLE
    private Double percent;

    public CourseMonthlyAttendanceDTO(Long studentId, String studentName, Long present, Long totalClasses, Double percent) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.present = present;
        this.totalClasses = totalClasses;
        this.percent = percent;
    }

    public Long getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public Long getPresent() { return present; }
    public Long getTotalClasses() { return totalClasses; }
    public Double getPercent() { return percent; }
}
