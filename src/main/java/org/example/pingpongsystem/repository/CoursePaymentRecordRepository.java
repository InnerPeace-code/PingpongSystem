package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CoursePaymentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoursePaymentRecordRepository extends JpaRepository<CoursePaymentRecordEntity, Long> {
    boolean existsByAppointmentId(Long appointmentId);

    Optional<CoursePaymentRecordEntity> findByAppointmentId(Long appointmentId);
}