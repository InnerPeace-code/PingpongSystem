package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.MonthlyMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyMatchRepository extends JpaRepository<MonthlyMatchEntity, Long> {
    List<MonthlyMatchEntity> findByYearAndMonth(Integer year, Integer month);
    // 新增：根据状态查询比赛
    List<MonthlyMatchEntity> findByStatus(MonthlyMatchEntity.MatchStatus status);
    List<MonthlyMatchEntity> findByYear(Integer year);
}