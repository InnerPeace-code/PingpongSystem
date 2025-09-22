package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CourseEvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseEvaluationRepository extends JpaRepository<CourseEvaluationEntity, Long> {
    // 根据预约ID查询评价
    List<CourseEvaluationEntity> findByAppointmentId(Long appointmentId);

    // 根据评价人ID和类型查询评价
    List<CourseEvaluationEntity> findByEvaluatorIdAndEvaluatorType(
            Long evaluatorId, CourseEvaluationEntity.EvaluatorType type);
}