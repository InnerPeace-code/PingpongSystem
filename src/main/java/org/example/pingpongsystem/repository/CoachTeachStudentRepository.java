package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CoachTeachStudentEntity;
import org.example.pingpongsystem.entity.SchoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoachTeachStudentRepository extends JpaRepository<CoachTeachStudentEntity, Long> {
    public int countByCoachId(Long coachId);
    public int countByStudentId(Long studentId);

    public List<CoachTeachStudentEntity> findByCoachId(Long coachId);

    // 2. 新增：根据教练ID和学员ID联合查询关联记录
    // Optional 表示“可能存在，也可能不存在”，避免返回 null
    Optional<CoachTeachStudentEntity> findByCoachIdAndStudentId(Long coachId, Long studentId);

    List<CoachTeachStudentEntity> findByStudentIdAndIsConfirmed(Long studentId, boolean isConfirmed);
    List<CoachTeachStudentEntity> findByCoachIdAndIsConfirmed(Long coachId,boolean isConfirmed);
    // 新增：验证已确认的双选关系
    boolean existsByCoachIdAndStudentIdAndIsConfirmed(Long coachId, Long studentId, boolean isConfirmed);

    // CoachTeachStudentRepository.java 新增方法
    long countByStudentIdAndCoachIdAndIsConfirmedTrue(Long studentId, Long coachId);
    List<CoachTeachStudentEntity> findByStudentIdAndIsConfirmedTrue(Long studentId);
    void deleteByStudentIdAndCoachId(Long studentId, Long coachId);
}
