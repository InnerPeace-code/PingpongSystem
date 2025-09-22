package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CoachEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CoachRepository extends JpaRepository<CoachEntity, Long>, JpaSpecificationExecutor<CoachEntity> {

    CoachEntity findByUsername(String name);

    List<CoachEntity> findAllByisCertified(boolean certified);

    List<CoachEntity> findBySchoolIdAndIsCertifiedTrue(Long schoolId);

    List<CoachEntity> findBySchoolId(Long schoolId); // 新增

    // 按校区ID分页查询已认证教练
    Page<CoachEntity> findBySchoolIdAndIsCertifiedTrue(Long schoolId, Pageable pageable);

    // 分页查询所有已认证教练
    Page<CoachEntity> findByIsCertifiedTrue(Pageable pageable);
}
