package com.marcedev.attendance.service;

import com.marcedev.attendance.dto.AttendanceDTO;
import com.marcedev.attendance.dto.AttendanceMarkDTO;
import com.marcedev.attendance.entities.ClassSession;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AttendanceService {

    AttendanceDTO save(AttendanceDTO dto);

    void registerAttendance(Long sessionId, List<AttendanceMarkDTO> attendances);

    List<AttendanceDTO> findAll();

    List<AttendanceDTO> findByClassId(Long classId);

    List<AttendanceDTO> findByCourseId(Long courseId);

    AttendanceDTO findById(Long id);

    void deleteById(Long id);

    void registerAttendanceByCourse(Long courseId, Map<Long, Boolean> attendanceMap);

    Optional<com.marcedev.attendance.entities.User> getAuthenticatedUserFromEmail(String email);


    ClassSession getOrCreateTodaySession(Long courseId);
}
