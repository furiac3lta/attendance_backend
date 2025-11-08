package com.marcedev.attendance.dto;

import com.marcedev.attendance.enums.Rol;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String fullName;
    private String email;
    private String role;

    // 🔹 Nuevos campos para multi-organización
    private Long organizationId;
    private String organizationName;
    private List<String> courses; // 👈 SOLO nombres

}
