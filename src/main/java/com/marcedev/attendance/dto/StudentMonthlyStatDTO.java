package com.marcedev.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentMonthlyStatDTO {


    private Long studentId;
    private String fullName;
    private Long presentCount;
    private Long absentCount;
    private Long total;
    private Double percent;

}
