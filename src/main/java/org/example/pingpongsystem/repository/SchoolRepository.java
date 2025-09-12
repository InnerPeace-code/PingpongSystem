package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.SchoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolRepository extends JpaRepository<SchoolEntity, Long> {

    SchoolEntity findBySchoolname(String name);
    // 新增：根据管理员ID查询关联的学校
    List<SchoolEntity> findByAdminId(Long adminId);
}
