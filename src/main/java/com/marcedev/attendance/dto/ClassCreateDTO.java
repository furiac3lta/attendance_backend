package com.marcedev.attendance.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class ClassCreateDTO {
    private String name;
    private String date;   // viene como string yyyy-MM-dd desde Angular
    private Long courseId;
}
