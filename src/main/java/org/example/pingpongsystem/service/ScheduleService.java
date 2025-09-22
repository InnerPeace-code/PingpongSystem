package org.example.pingpongsystem.service;

import org.example.pingpongsystem.dto.SchoolDTO;
import org.example.pingpongsystem.entity.ScheduleEntity;
import org.example.pingpongsystem.entity.SchoolEntity;
import org.example.pingpongsystem.repository.ScheduleRepository;
import org.example.pingpongsystem.repository.SchoolRepository;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final SchoolRepository schoolRepository;

    public ScheduleService(ScheduleRepository scheduleRepository, SchoolRepository schoolRepository) {
        this.scheduleRepository = scheduleRepository;
        this.schoolRepository = schoolRepository;
    }

    // 获取默认课表模板（1=周一，...，6=周六，7=周日）
    public Result<List<ScheduleEntity>> getDefaultSchedule() {
        List<ScheduleEntity> defaultSchedule = new ArrayList<>();
        // 数组索引0对应周一（1），索引5对应周六（6），索引6对应周日（7）
        int[] weekdays = {1, 2, 3, 4, 5, 6, 7};

        for (int day : weekdays) {
            ScheduleEntity schedule = new ScheduleEntity();
            schedule.setDayOfWeek(day); // 存储整数（1-7）
            schedule.setStartTime(LocalTime.of(9, 0));
            schedule.setEndTime(LocalTime.of(18, 0));
            schedule.setDescription("默认训练时间");
            defaultSchedule.add(schedule);
        }
        return Result.success(defaultSchedule);
    }

    // 检查校区是否已有课表
    public Result<Boolean> hasSchedule(Long schoolId) {
        return Result.success(!scheduleRepository.findBySchoolId(schoolId).isEmpty());
    }

    // 保存课表到指定校区
    @Transactional
    public Result<Void> saveSchedule(Long schoolId, List<ScheduleEntity> schedules) {
        // 先删除原有课表
        scheduleRepository.deleteBySchoolId(schoolId);

        // 保存新课表
        schedules.forEach(schedule -> {
            schedule.setSchoolId(schoolId);
            scheduleRepository.save(schedule);
        });
        return Result.success();
    }

    // 获取校区课表
    public Result<List<ScheduleEntity>> getSchoolSchedule(Long schoolId) {
        return Result.success(scheduleRepository.findBySchoolId(schoolId));
    }

    // 超级管理员获取所有校区
    public Result<List<SchoolEntity>> getAllSchools() {
        return Result.success(schoolRepository.findAll());
    }

    // 管理员获取管辖校区
    public Result<List<SchoolEntity>> getManagedSchools(Long adminId) {
        return Result.success(schoolRepository.findByAdminId(adminId));
    }

    // 超级管理员：获取所有已有课表的校区（作为模板选择项）
    public Result<List<SchoolDTO>> getSchoolsWithSchedule() {
        // 查询所有校区，过滤出有课表的校区
        List<SchoolEntity> allSchools = schoolRepository.findAll();
        List<SchoolDTO> result = new ArrayList<>();
        for (SchoolEntity school : allSchools) {
            if (!scheduleRepository.findBySchoolId(school.getId()).isEmpty()) {
                result.add(new SchoolDTO(school.getId(), school.getSchoolname(), school.getAdminId()));
            }
        }
        return Result.success(result);
    }

    // 普通管理员：获取自己管辖的、已有课表的校区（作为模板选择项）
    public Result<List<SchoolDTO>> getManagedSchoolsWithSchedule(Long adminId) {
        // 查询该管理员管辖的校区，过滤出有课表的校区
        List<SchoolEntity> managedSchools = schoolRepository.findByAdminId(adminId);
        List<SchoolDTO> result = new ArrayList<>();
        for (SchoolEntity school : managedSchools) {
            if (!scheduleRepository.findBySchoolId(school.getId()).isEmpty()) {
                result.add(new SchoolDTO(school.getId(), school.getSchoolname(), school.getAdminId()));
            }
        }
        return Result.success(result);
    }
}