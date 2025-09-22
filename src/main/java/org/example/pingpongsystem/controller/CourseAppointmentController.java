package org.example.pingpongsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.CancelRecordEntity;
import org.example.pingpongsystem.entity.CourseAppointmentEntity;
import org.example.pingpongsystem.service.CourseAppointmentService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RequestMapping("/appointment")
@RestController
@RequiredArgsConstructor
public class CourseAppointmentController {

    private final CourseAppointmentService appointmentService;

    // 获取教练课表
    @GetMapping("/coach_schedule")
    public Result<List<CourseAppointmentEntity>> getCoachSchedule(@RequestParam Long coachId) {
        return appointmentService.getCoachSchedule(coachId);
    }

    // 学员预约课程
    @PostMapping("/book")
    public Result<CourseAppointmentEntity> bookCourse(
            @RequestParam Long coachId,
            @RequestParam Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Long tableId,
            @RequestParam boolean autoAssign) {
        return appointmentService.bookCourse(coachId, studentId, startTime, endTime, tableId, autoAssign);
    }

    // 教练处理预约
    @PostMapping("/coach_handle")
    public Result<String> handleCoachConfirmation(
            @RequestParam Long appointmentId,
            @RequestParam boolean accept) {
        return appointmentService.handleCoachConfirmation(appointmentId, accept);
    }

    // 发起取消申请
    @PostMapping("/cancel_request")
    public Result<String> requestCancel(
            @RequestParam Long appointmentId,
            @RequestParam Long userId,
            @RequestParam String userType) {  // STUDENT/COACH
        return appointmentService.requestCancel(appointmentId, userId, userType);
    }

    // 处理取消申请
    @PostMapping("/handle_cancel")
    public Result<String> handleCancelRequest(
            @RequestParam Long cancelRecordId,
            @RequestParam boolean approve) {
        return appointmentService.handleCancelRequest(cancelRecordId, approve);
    }

    @GetMapping("/student_list")
    public Result<List<CourseAppointmentEntity>> getStudentAppointments(@RequestParam Long studentId) {
        return appointmentService.getStudentAppointments(studentId);
    }

    @GetMapping("/coach_list")
    public Result<List<CourseAppointmentEntity>> getCoachAppointments(@RequestParam Long coachId){
        return appointmentService.getCoachAppointments(coachId);
    }

    @GetMapping("/pending_cancel_records")
    public Result<List<CancelRecordEntity>> getPendingCancelRecords(
            @RequestParam Long userId,
            @RequestParam String userType) {
        return appointmentService.getPendingCancelRecords(userId, userType);
    }

    // 获取本月剩余取消次数
    @GetMapping("/remaining_cancel_count")
    public Result<Integer> getRemainingCancelCount(
            @RequestParam Long userId,
            @RequestParam String userType) {  // STUDENT/COACH
        return appointmentService.getRemainingCancelCount(userId, userType);
    }
}