package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CourseAppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CourseAppointmentRepository extends JpaRepository<CourseAppointmentEntity, Long> {
    // 查询教练未来一周的预约
    List<CourseAppointmentEntity> findByCoachIdAndStartTimeBetween(
            Long coachId, LocalDateTime start, LocalDateTime end);

    // 查询学员的预约
    List<CourseAppointmentEntity> findByStudentId(Long studentId);

    // 查询球台在指定时间段的预约
    List<CourseAppointmentEntity> findByTableIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long tableId, LocalDateTime endTime, LocalDateTime startTime);
    //查询教练的预约
    List<CourseAppointmentEntity> findByCoachId(Long coachId);
}