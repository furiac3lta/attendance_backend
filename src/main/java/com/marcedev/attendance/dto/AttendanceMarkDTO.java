package com.marcedev.attendance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceMarkDTO {
    private Long userId;     // 👈 coincide con el frontend
    private boolean present; // 👈 coincide con el frontend
}
