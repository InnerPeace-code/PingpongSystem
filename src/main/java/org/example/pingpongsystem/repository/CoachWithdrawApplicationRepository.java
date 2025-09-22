package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CoachWithdrawApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoachWithdrawApplicationRepository extends JpaRepository<CoachWithdrawApplicationEntity, Long> {
    // 按教练ID查询提现申请
    List<CoachWithdrawApplicationEntity> findByCoachIdOrderByCreateTimeDesc(Long coachId);
}
