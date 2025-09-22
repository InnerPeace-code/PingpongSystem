package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CoachChangeRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoachChangeRequestRepository extends JpaRepository<CoachChangeRequestEntity, Long> {
    // 查询学员的申请记录
    List<CoachChangeRequestEntity> findByStudentId(Long studentId);

    // 查询当前教练待处理的申请
    List<CoachChangeRequestEntity> findByCurrentCoachIdAndStatus(Long coachId, CoachChangeRequestEntity.Status status);

    // 查询目标教练待处理的申请
    List<CoachChangeRequestEntity> findByTargetCoachIdAndStatus(Long coachId, CoachChangeRequestEntity.Status status);

    // 查询校区管理员待处理的申请
    List<CoachChangeRequestEntity> findBySchoolIdAndStatus(Long schoolId, CoachChangeRequestEntity.Status status);

    // 检查是否有未完成的申请
    Optional<CoachChangeRequestEntity> findByStudentIdAndStatusNot(Long studentId, CoachChangeRequestEntity.Status status);

    // 新增：按学员ID和状态查询（用于查找待处理的申请）
    Optional<CoachChangeRequestEntity> findByStudentIdAndStatus(Long studentId, CoachChangeRequestEntity.Status status);

}