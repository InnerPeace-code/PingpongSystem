package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.dto.SchoolDTO;
import org.example.pingpongsystem.entity.ScheduleEntity;
import org.example.pingpongsystem.entity.SchoolEntity;
import org.example.pingpongsystem.service.ScheduleService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/super_admin/schedule")
@RestController
public class SuperAdminScheduleController {
    private final ScheduleService scheduleService;

    public SuperAdminScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // 获取默认课表模板
    @GetMapping("/default")
    public Result<List<ScheduleEntity>> getDefaultSchedule() {
        return scheduleService.getDefaultSchedule();
    }

    // 获取所有校区
    @GetMapping("/schools")
    public Result<List<SchoolEntity>> getAllSchools() {
        return scheduleService.getAllSchools();
    }

    // 检查校区是否已有课表
    @GetMapping("/check/{schoolId}")
    public Result<Boolean> checkSchoolSchedule(@PathVariable Long schoolId) {
        return scheduleService.hasSchedule(schoolId);
    }

    // 保存课表到指定校区
    @PostMapping("/save/{schoolId}")
    public Result<Void> saveSchedule(
            @PathVariable Long schoolId,
            @RequestBody List<ScheduleEntity> schedules
    ) {
        return scheduleService.saveSchedule(schoolId, schedules);
    }

    // 获取校区课表
    @GetMapping("/school/{schoolId}")
    public Result<List<ScheduleEntity>> getSchoolSchedule(@PathVariable Long schoolId) {
        return scheduleService.getSchoolSchedule(schoolId);
    }

    // 获取已有课表的校区列表（作为现有模板选择项）
    @GetMapping("/existing-templates")
    public Result<List<SchoolDTO>> getExistingTemplateSchools() {
        return scheduleService.getSchoolsWithSchedule();
    }

    // 根据校区ID获取该校区的课表（作为模板使用）
    @GetMapping("/template/{schoolId}")
    public Result<List<ScheduleEntity>> getScheduleTemplate(@PathVariable Long schoolId) {
        return scheduleService.getSchoolSchedule(schoolId); // 复用已有方法
    }
}