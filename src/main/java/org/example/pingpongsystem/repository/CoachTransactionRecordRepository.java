package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CoachTransactionRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachTransactionRecordRepository extends JpaRepository<CoachTransactionRecordEntity, Long> {
    // 按教练ID查询交易记录，分页
    Page<CoachTransactionRecordEntity> findByCoachIdOrderByCreateTimeDesc(Long coachId, Pageable pageable);

    // 按教练ID和交易类型查询，分页
    Page<CoachTransactionRecordEntity> findByCoachIdAndTypeOrderByCreateTimeDesc(
            Long coachId, CoachTransactionRecordEntity.TransactionType type, Pageable pageable);
}


