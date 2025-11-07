package com.marcedev.attendance.security.model;

import com.marcedev.attendance.dto.UserDTO;
import com.marcedev.attendance.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type; // Por ejemplo: ADMIN o MEMBER
    private UserDTO user; // 👈 tu DTO ya existente


}
