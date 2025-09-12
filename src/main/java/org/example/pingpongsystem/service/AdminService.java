package org.example.pingpongsystem.service;

import jakarta.validation.ConstraintViolationException;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.repository.*;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.example.pingpongsystem.utility.interfaces.InfoAns;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private final SchoolRepository schoolRepository;
    private final TableRepository tableRepository;
    private final AdminRepository adminRepository;
    private final CoachRepository coachRepository;
    private final TokenService tokenService;

    public AdminService(SchoolRepository schoolRepository, TableRepository tableRepository, AdminRepository adminRepository, CoachRepository coachRepository, TokenService tokenService) {
        this.schoolRepository = schoolRepository;
        this.tableRepository = tableRepository;
        this.adminRepository = adminRepository;
        this.coachRepository = coachRepository;
        this.tokenService = tokenService;
    }

    public Result<String> login(String username, String password) {
        AdminEntity temp = adminRepository.findByUsername(username);
        if (temp == null) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "用户名不存在");
        }
        else if (!temp.getPassword().equals(password)) {
            return Result.error(StatusCode.PASSWORD_ERROR, "密码错误");
        }
        return tokenService.createToken(false, true, false, false, temp.getId());
    }

    public Result<List<CoachEntity>> getUncertifiedCoachesByAdminToken(String token) {
        // 1. 通过token解析出当前管理员的ID
        Result<InfoAns> infoResult = tokenService.getInfo(token);
        if (!infoResult.isSuccess()) {
            return Result.error(StatusCode.FAIL, "获取管理员信息失败：" + infoResult.getMessage());
        }
        InfoAns adminInfo = infoResult.getData();
        // 验证当前用户是否为管理员（避免越权）
        if (!"admin".equals(adminInfo.getRole()) && !"super_admin".equals(adminInfo.getRole())) {
            return Result.error(StatusCode.FAIL, "权限不足，非管理员用户");
        }
        Long adminId = Long.valueOf(adminInfo.getUserId()); // 假设InfoAns中已包含userId字段（需确认实体定义）

        // 2. 查询该管理员管理的所有校区（通过adminId关联）
        List<SchoolEntity> managedSchools = schoolRepository.findByAdminId(adminId);
        if (managedSchools.isEmpty()) {
            return Result.success(new ArrayList<>()); // 没有管理的校区，返回空列表
        }

        // 3. 提取所有校区ID
        List<Long> schoolIds = managedSchools.stream()
                .map(SchoolEntity::getId)
                .collect(Collectors.toList());

        // 4. 查询这些校区下的所有未审核教练
        List<CoachEntity> allUncertified = coachRepository.findAllByisCertified(false);
        List<CoachEntity> result = allUncertified.stream()
                .filter(coach -> schoolIds.contains(coach.getSchoolId()))
                .collect(Collectors.toList());

        return Result.success(result);
    }

    public Result<CoachEntity> getCoachDetail(Long coachId) {
        Optional<CoachEntity> coachOpt = coachRepository.findById(coachId);
        if (coachOpt.isPresent()) {
            CoachEntity coach = coachOpt.get();
            // 确保只返回未审核的教练详情（权限控制）
            if (!coach.isCertified()) {
                return Result.success(coach);
            } else {
                return Result.error(StatusCode.FAIL, "该教练已通过审核，无需查看");
            }
        } else {
            return Result.error(StatusCode.FAIL, "未找到该教练信息");
        }
    }

    @Transactional
    public Result<CoachEntity> certifyCoach(Long coachId, boolean isAccepted,int level) {
        Optional<CoachEntity> tmp = coachRepository.findById(coachId);
        if (tmp.isPresent()) {
            CoachEntity coach = tmp.get();
            if (isAccepted) {
                coach.setCertified(true);
                coach.setLevel(level);
                System.out.println(coach.getName());
                return Result.success(coach);
            }
            else {
                coachRepository.delete(coach);
                return Result.success();
            }
        }
        else return Result.error(StatusCode.FAIL, "该教练不存在");
    }

    @Transactional
    public Result<SchoolEntity> reviseSchool(SchoolEntity school) {
        SchoolEntity temp = schoolRepository.findBySchoolname(school.getSchoolname());
        if (temp == null) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "学校不存在");
        }
        else {
            if (!school.getAddress().equals(temp.getAddress())) {
                if (!school.getAddress().isEmpty())
                    temp.setAddress(school.getAddress());
            }
            if (!school.getName().equals(temp.getName())) {
                if (!school.getName().isEmpty())
                    temp.setName(school.getName());
            }
            if (!school.getPhone().equals(temp.getPhone())) {
                if (!school.getPhone().isEmpty())
                    temp.setPhone(school.getPhone());
            }
            if (!school.getEmail().equals(temp.getEmail())) {
                if (!school.getEmail().isEmpty())
                    temp.setEmail(school.getEmail());
            }
            if (school.getAdminId() != temp.getAdminId()) {
                temp.setAdminId(school.getAdminId());
            }
            if (school.getTable_num() != temp.getTable_num()) {
                List<TableEntity> li = tableRepository.findAllBySchoolId(school.getId());
                tableRepository.deleteAll(li);

                for (int i=0;i<school.getTable_num();i++) {
                    TableEntity tableEntity = new TableEntity();
                    tableEntity.setOccupied(false);
                    tableEntity.setSchoolId(school.getId());
                    tableRepository.save(tableEntity);
                }

                temp.setTable_num(school.getTable_num());
            }
            return Result.success(temp);
        }
    }
}
