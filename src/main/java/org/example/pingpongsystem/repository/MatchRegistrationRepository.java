package org.example.pingpongsystem.repository;


import org.example.pingpongsystem.entity.MatchRegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRegistrationRepository extends JpaRepository<MatchRegistrationEntity, Long> {
    Optional<MatchRegistrationEntity> findByMonthlyMatchIdAndStudentId(Long matchId, Long studentId);
    List<MatchRegistrationEntity> findByMonthlyMatchIdAndGroupType(Long matchId, MatchRegistrationEntity.GroupType groupType);
    List<MatchRegistrationEntity> findByStudentId(Long studentId);
    long countByMonthlyMatchIdAndGroupType(Long matchId, MatchRegistrationEntity.GroupType groupType);
}
