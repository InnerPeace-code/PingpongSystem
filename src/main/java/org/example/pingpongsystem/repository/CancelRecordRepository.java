package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CancelRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CancelRecordRepository extends JpaRepository<CancelRecordEntity, Long> {
    // 统计学生本月的有效取消次数
    long countByStudentIdAndUserTypeAndStatusInAndCreateTimeBetween(
            Long studentId, String userType, List<CancelRecordEntity.CancelStatus> statuses,
            LocalDateTime monthStart, LocalDateTime monthEnd);
    // 统计教练本月的有效取消次数
    long countByCoachIdAndUserTypeAndStatusInAndCreateTimeBetween(
            Long coachId, String userType, List<CancelRecordEntity.CancelStatus> statuses,
            LocalDateTime monthStart, LocalDateTime monthEnd);

    // 查询预约相关的取消记录
    CancelRecordEntity findByAppointmentId(Long appointmentId);

    // 获取待处理的取消申请（自己是应答人，对方是发起人，状态为待处理）
    List<CancelRecordEntity> findByCoachIdAndUserTypeAndStatus(
            Long coachId, String userType, CancelRecordEntity.CancelStatus status);

    List<CancelRecordEntity> findByStudentIdAndUserTypeAndStatus(
            Long studentId, String userType, CancelRecordEntity.CancelStatus status);
}