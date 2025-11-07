package com.marcedev.attendance.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClassDTO {

    private Long id;
    private String name;
    private LocalDate date;

    private Long courseId;
    private String courseName;

    private Long instructorId;
    private String instructorName;

    // 🔹 Nuevos campos para multi-organización (gimnasio / colegio / instituto)
    private Long organizationId;
    private String organizationName;
}
