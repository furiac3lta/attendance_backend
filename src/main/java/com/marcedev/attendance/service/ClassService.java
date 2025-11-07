package com.marcedev.attendance.service;

import com.marcedev.attendance.dto.ClassDetailsDTO;
import com.marcedev.attendance.entities.ClassSession;
import com.marcedev.attendance.entities.Course;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassService {

    ClassSession create(ClassSession newClass);

    ClassSession findById(Long id);

    List<ClassSession> findByCourseId(Long courseId);

    ClassDetailsDTO getClassDetails(Long classId);

    Course getCourseById(Long courseId);

    List<ClassSession> findByOrganization(Long organizationId);

    Optional<ClassSession> findByCourseIdAndDate(Long courseId, LocalDate date);

    ClassSession getOrCreateTodaySession(Long courseId);


}
