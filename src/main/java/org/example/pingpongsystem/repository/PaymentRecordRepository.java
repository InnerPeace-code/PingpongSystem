package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.PaymentRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecordEntity, Long> {
    // 按学生ID查询所有记录
    List<PaymentRecordEntity> findByStudentId(Long studentId);

    // 按条件分页查询
    Page<PaymentRecordEntity> findAll(Specification<PaymentRecordEntity> spec, Pageable pageable);
}
