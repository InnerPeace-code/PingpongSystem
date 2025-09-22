package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CoachAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoachAccountRepository extends JpaRepository<CoachAccountEntity, Long> {
    Optional<CoachAccountEntity> findByCoachId(Long coachId);
    boolean existsByCoachId(Long coachId);
}
