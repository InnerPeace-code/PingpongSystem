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

import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {
    private final TokenRepository tokenRepository;
    private final SuperAdminRepository superAdminRepository;
    private final AdminRepository adminRepository;
    private final CoachRepository coachRepository;
    private final StudentRepository studentRepository;

    public TokenService(TokenRepository tokenRepository, SuperAdminRepository superAdminRepository, AdminRepository adminRepository, CoachRepository coachRepository, StudentRepository studentRepository) {
        this.tokenRepository = tokenRepository;
        this.superAdminRepository = superAdminRepository;
        this.adminRepository = adminRepository;
        this.coachRepository = coachRepository;
        this.studentRepository = studentRepository;
    }

    public Result<InfoAns> getInfo(String token) {
        TokenEntity tokenEntity = tokenRepository.findByToken(token);
        if (tokenEntity != null) {
            // 1. 超级管理员查询：使用userId
            if (tokenEntity.isSuperAdmin()) {
                Optional<SuperAdminEntity> admin = superAdminRepository.findById(tokenEntity.getUserId());
                if (admin.isPresent()) {
                    InfoAns infoAns = new InfoAns();
                    infoAns.setUsername(admin.get().getUsername());
                    infoAns.setPassword(admin.get().getPassword());
                    infoAns.setPhone(admin.get().getPhone());
                    infoAns.setEmail(admin.get().getEmail());
                    // 补充角色信息（前端需要role字段）
                    infoAns.setRole("super_admin");
                    infoAns.setUserId(tokenEntity.getUserId());
                    return Result.success(infoAns);
                } else return Result.error(StatusCode.FAIL, "超级管理员未找到");
            }
            // 2. 管理员查询：使用userId
            if (tokenEntity.isAdmin()) {
                Optional<AdminEntity> admin = adminRepository.findById(tokenEntity.getUserId());
                if (admin.isPresent()) {
                    InfoAns infoAns = new InfoAns();
                    infoAns.setUsername(admin.get().getUsername());
                    infoAns.setPassword(admin.get().getPassword());
                    infoAns.setPhone(admin.get().getPhone());
                    infoAns.setEmail(admin.get().getEmail());
                    // 补充角色信息
                    infoAns.setRole("admin");
                    infoAns.setUserId(tokenEntity.getUserId());
                    return Result.success(infoAns);
                } else return Result.error(StatusCode.FAIL, "管理员未找到");
            }
            // 3. 教练查询：使用userId
            if (tokenEntity.isCoach()) {
                Optional<CoachEntity> coach = coachRepository.findById(tokenEntity.getUserId());
                if (coach.isPresent()) {
                    InfoAns infoAns = new InfoAns();
                    infoAns.setUsername(coach.get().getUsername());
                    infoAns.setPassword(coach.get().getPassword());
                    infoAns.setAge(coach.get().getAge());
                    infoAns.setMale(coach.get().isMale());
                    infoAns.setPhone(coach.get().getPhone());
                    infoAns.setEmail(coach.get().getEmail());
                    infoAns.setSchoolId(coach.get().getSchoolId());
                    // 补充角色信息
                    infoAns.setRole("coach");
                    infoAns.setUserId(tokenEntity.getUserId());
                    return Result.success(infoAns);
                } else return Result.error(StatusCode.FAIL, "教练未找到");
            }
            // 4. 学员查询：使用userId
            if (tokenEntity.isStudent()) {
                Optional<StudentEntity> student = studentRepository.findById(tokenEntity.getUserId());
                if (student.isPresent()) {
                    InfoAns infoAns = new InfoAns();
                    infoAns.setUsername(student.get().getUsername());
                    infoAns.setPassword(student.get().getPassword());
                    infoAns.setAge(student.get().getAge());
                    infoAns.setMale(student.get().isMale());
                    infoAns.setPhone(student.get().getPhone());
                    infoAns.setEmail(student.get().getEmail());
                    infoAns.setSchoolId(student.get().getSchoolId());
                    // 补充角色信息
                    infoAns.setRole("student");
                    infoAns.setUserId(tokenEntity.getUserId());
                    return Result.success(infoAns);
                } else return Result.error(StatusCode.FAIL, "学员未找到");
            }
            return Result.error(StatusCode.FAIL, "检查token数据表（角色标识异常）");
        } else {
            return Result.error(StatusCode.FAIL, "token不存在");
        }
    }
    public Result<String> createToken(boolean isSuperAdmin, boolean isAdmin, boolean isCoach, boolean isStudent, Long userId) {
        try {
            TokenEntity tokenEntity = new TokenEntity();
            tokenEntity.setToken(UUID.randomUUID().toString());
            tokenEntity.setSuperAdmin(isSuperAdmin);
            tokenEntity.setAdmin(isAdmin);
            tokenEntity.setCoach(isCoach);
            tokenEntity.setStudent(isStudent);
            tokenEntity.setUserId(userId);
            tokenRepository.save(tokenEntity);
            return Result.success(tokenEntity.getToken());
        } catch (OptimisticLockingFailureException e) {
            System.err.println("数据已被其他用户修改，请刷新后重试");
            return Result.error(StatusCode.FAIL, "数据已被其他用户修改，请刷新后重试");
        } catch (ConstraintViolationException e) {
            System.err.println("必需字段空缺");
            return Result.error(StatusCode.FAIL, "必需字段空缺");
        } catch (DataAccessException e) {
            System.err.println("保存token信息失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "保存token信息失败");
        }
    }

    @Transactional
    public Result<String> logout(String token) {
        try {
            // 先查询token是否存在
            System.out.println(token);
            TokenEntity tokenEntity = tokenRepository.findByToken(token);
            if (tokenEntity == null) {
                return Result.error(StatusCode.FAIL, "token不存在");
            }
            // 删除token记录
            tokenRepository.deleteByToken(token);
            return Result.success("success"); // 与前端mock返回格式一致
        } catch (DataAccessException e) {
            System.err.println("登出失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "登出失败");
        }
    }
}
