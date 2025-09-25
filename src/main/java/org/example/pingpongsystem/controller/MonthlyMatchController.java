package org.example.pingpongsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.MatchRegistrationEntity;
import org.example.pingpongsystem.entity.MatchScheduleEntity;
import org.example.pingpongsystem.entity.MonthlyMatchEntity;
import org.example.pingpongsystem.service.MonthlyMatchService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RequestMapping("/monthly-match")
@RestController
@RequiredArgsConstructor
public class MonthlyMatchController {

    private final MonthlyMatchService matchService;

    // 获取当前可报名的月赛
    @GetMapping("/available")
    public Result<List<MonthlyMatchEntity>> getAvailableMatches() {
        return matchService.getAvailableMatches();
    }

    // 学员报名月赛
    @PostMapping("/register")
    public Result<MatchRegistrationEntity> register(
            @RequestParam Long matchId,
            @RequestParam Long studentId,
            @RequestParam String groupType) { // GROUP_A, GROUP_B, GROUP_C
        return matchService.registerForMatch(matchId, studentId, MatchRegistrationEntity.GroupType.valueOf(groupType));
    }

    // 获取学员的报名记录
    @GetMapping("/student-registrations")
    public Result<List<MatchRegistrationEntity>> getStudentRegistrations(@RequestParam Long studentId) {
        return matchService.getStudentRegistrations(studentId);
    }

    // 获取月赛各组别报名人数
    @GetMapping("/group-counts")
    public Result<Map<MatchRegistrationEntity.GroupType, Long>> getGroupCounts(@RequestParam Long matchId) {
        return matchService.getGroupRegistrationCounts(matchId);
    }

    // 获取学员的比赛安排
    @GetMapping("/student-schedule")
    public Result<List<MatchScheduleEntity>> getStudentSchedule(
            @RequestParam Long matchId,
            @RequestParam Long studentId) {
        return matchService.getStudentMatchSchedule(matchId, studentId);
    }

    // 管理员修改比赛时间
    @PostMapping("/admin/update-time")
    public Result<MonthlyMatchEntity> updateMatchTime(
            @RequestParam Long matchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime) {
        return matchService.updateMatchTime(matchId, startTime);
    }

    /**
     * 获取单个月赛详情
     */
    @GetMapping("/get-match")
    public Result<MonthlyMatchEntity> getMatchById(@RequestParam Long matchId) {
        return matchService.getMatchById(matchId);
    }


    /**
     * 管理员创建新比赛
     */
    @PostMapping("/admin/create")
    public Result<MonthlyMatchEntity> createMatch(
            @RequestParam String title,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime registrationDeadline) {
        return matchService.createMatchByAdmin(title, startTime, registrationDeadline);
    }

    /**
     * 管理员更新比赛信息（包括状态、时间等）
     */
    @PutMapping("/admin/update")
    public Result<MonthlyMatchEntity> updateMatch(
            @RequestParam Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime registrationDeadline,
            @RequestParam(required = false) String status) { // 状态参数：NOT_STARTED, REGISTERING, REGISTRATION_CLOSED, ONGOING, COMPLETED
        MonthlyMatchEntity.MatchStatus matchStatus = null;
        if (status != null && !status.isEmpty()) {
            matchStatus = MonthlyMatchEntity.MatchStatus.valueOf(status);
        }
        return matchService.updateMatchByAdmin(id, title, startTime, registrationDeadline, matchStatus);
    }

    /**
     * 管理员获取所有比赛（包括已结束的）
     */
    @GetMapping("/admin/all")
    public Result<List<MonthlyMatchEntity>> getAllMatches(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return matchService.getAllMatches(year, month);
    }

    /**
     * 管理员手动触发赛程安排
     */
    @PostMapping("/admin/arrange-schedule")
    public Result<Void> manuallyArrangeSchedule(@RequestParam Long matchId) {
        return matchService.manuallyArrangeSchedule(matchId);
    }

    /**
     * 新增：管理员获取某比赛的全量赛程
     * @param matchId 月赛ID
     */
    @GetMapping("/admin/schedule")
    public Result<List<MatchScheduleEntity>> getAdminMatchSchedule(
            @RequestParam Long matchId) {
        return matchService.getAdminMatchSchedule(matchId);
    }
}
