package com.marcedev.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO que representa los detalles básicos de una clase (ClassSession)
 * Incluye información mínima para vistas o listados rápidos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassDetailsDTO {

    /** ID de la clase */
    private Long id;

    /** Nombre o título de la clase */
    private String className;

    /** Fecha de dictado */
    private LocalDate date;

    /** Nombre del curso asociado */
    private String courseName;
}
