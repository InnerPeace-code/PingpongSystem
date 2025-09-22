package org.example.pingpongsystem.service;

import org.example.pingpongsystem.dto.SchoolDTO;
import org.example.pingpongsystem.entity.SchoolEntity;
import org.example.pingpongsystem.repository.SchoolRepository;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommonService {  // 建议放在公共服务类中

    private final SchoolRepository schoolRepository;

    // 构造函数注入依赖
    public CommonService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public Result<List<SchoolDTO>> getSchoolIdAndNames() {
        try {
            List<SchoolEntity> schools = schoolRepository.findAll();
            List<SchoolDTO> result = schools.stream()
                    .map(school -> new SchoolDTO(school.getId(), school.getSchoolname(),school.getAdminId()))
                    .collect(Collectors.toList());
            return Result.success(result);
        } catch (DataAccessException e) {
            System.err.println("获取学校列表失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "获取学校列表失败");
        }
    }
}