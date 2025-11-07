package com.marcedev.attendance.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String role; // 👈 Nuevo campo: "ADMIN", "INSTRUCTOR", "MEMBER"

}
