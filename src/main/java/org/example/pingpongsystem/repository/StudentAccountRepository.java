package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.PaymentRecordEntity;
import org.example.pingpongsystem.entity.StudentAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentAccountRepository extends JpaRepository<StudentAccountEntity, Long> {
    Optional<StudentAccountEntity> findByStudentId(Long studentId);
}