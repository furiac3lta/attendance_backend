package com.marcedev.attendance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrganizationDTO {
    private Long id;
    private String name;
    private String type;
    private String phone;
    private String address;
    private String logoUrl;
    private String adminFullName; // 👈 agregar este campo

}
