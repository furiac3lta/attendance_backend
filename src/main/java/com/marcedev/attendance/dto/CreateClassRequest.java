package com.marcedev.attendance.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


public record CreateClassRequest(
        Long instructorId,
        Long courseId,
        String name,
        LocalDate date
) {}
