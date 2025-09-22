package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.StudentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface StudentRepository extends JpaRepository<StudentEntity, Long>, JpaSpecificationExecutor<StudentEntity> {

    StudentEntity findByUsername(String name);

    // 未分页：按校区ID查询所有学生（返回List）
    List<StudentEntity> findBySchoolId(Long schoolId);

    // 分页：按校区ID查询学生（返回Page）
    Page<StudentEntity> findBySchoolId(Long schoolId, Pageable pageable);

}
