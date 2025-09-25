package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.MatchScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchScheduleRepository extends JpaRepository<MatchScheduleEntity, Long> {
    List<MatchScheduleEntity> findByMonthlyMatchId(Long matchId);
    List<MatchScheduleEntity> findByMonthlyMatchIdAndGroupId(Long matchId, Long groupId);
    // 方法名含义：查询“monthlyMatchId=?1 且 (player1Id=?2 或 player2Id=?2)”的记录
    List<MatchScheduleEntity> findByMonthlyMatchIdAndPlayer1IdOrMonthlyMatchIdAndPlayer2Id(
            Long monthlyMatchId,
            Long player1Id,
            Long monthlyMatchId2,  // 与第一个monthlyMatchId参数值相同
            Long player2Id
    );
    long countByMonthlyMatchId(Long MonthlyMatchId);
}