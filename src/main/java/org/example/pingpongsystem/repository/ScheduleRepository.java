package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {
    // 根据校区ID查询课表
    List<ScheduleEntity> findBySchoolId(Long schoolId);

    // 批量删除校区课表
    void deleteBySchoolId(Long schoolId);
}