package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.MatchGroupEntity;
import org.example.pingpongsystem.entity.MatchRegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchGroupRepository extends JpaRepository<MatchGroupEntity, Long> {
    List<MatchGroupEntity> findByMonthlyMatchIdAndGroupType(Long matchId, MatchRegistrationEntity.GroupType groupType);
}