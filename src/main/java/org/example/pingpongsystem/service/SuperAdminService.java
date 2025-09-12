package org.example.pingpongsystem.service;

import jakarta.validation.ConstraintViolationException;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.repository.*;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SuperAdminService {
    private final SuperAdminRepository superAdminRepository;
    private final SchoolRepository schoolRepository;
    private final TableRepository tableRepository;
    private final AdminRepository adminRepository;
    private final TokenService tokenService;
    private final CoachRepository coachRepository;

    public SuperAdminService(SuperAdminRepository superAdminRepository, SchoolRepository schoolRepository, TableRepository tableRepository, AdminRepository adminRepository, TokenService tokenService, CoachRepository coachRepository) {
        this.superAdminRepository = superAdminRepository;
        this.schoolRepository = schoolRepository;
        this.tableRepository = tableRepository;
        this.adminRepository = adminRepository;
        this.tokenService = tokenService;
        this.coachRepository = coachRepository;
    }

    public Result<AdminEntity> createAdmin(AdminEntity admin) {
        try {
            adminRepository.save(admin);
            return Result.success(admin);
        } catch (OptimisticLockingFailureException e) {
            System.err.println("数据已被其他用户修改，请刷新后重试");
            return Result.error(StatusCode.FAIL, "数据已被其他用户修改，请刷新后重试");
        } catch (ConstraintViolationException e) {
            System.err.println("必需字段空缺");
            return Result.error(StatusCode.FAIL, "必需字段空缺");
        } catch (DataAccessException e) {
            System.err.println("创建管理员失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "创建管理员失败");
        }
    }

    // 新增：获取所有管理员
    public Result<List<AdminEntity>> getAllAdmins() {
        try {
            List<AdminEntity> admins = adminRepository.findAll();
            return Result.success(admins);
        } catch (DataAccessException e) {
            System.err.println("获取管理员列表失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "获取管理员列表失败");
        }
    }

    // 新增：编辑管理员
    @Transactional
    public Result<AdminEntity> editAdmin(AdminEntity admin) {
        try {
            Optional<AdminEntity> existingAdmin = adminRepository.findById(admin.getId());
            if (existingAdmin.isEmpty()) {
                return Result.error(StatusCode.USERNAME_NOT_FOUND, "管理员不存在");
            }

            AdminEntity updateAdmin = existingAdmin.get();
            // 更新可编辑字段
            if (admin.getUsername() != null && !admin.getUsername().isEmpty()) {
                updateAdmin.setUsername(admin.getUsername());
            }
            if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
                updateAdmin.setPassword(admin.getPassword());
            }
            if (admin.getName() != null && !admin.getName().isEmpty()) {
                updateAdmin.setName(admin.getName());
            }
            if (admin.getPhone() != null && !admin.getPhone().isEmpty()) {
                updateAdmin.setPhone(admin.getPhone());
            }
            if (admin.getEmail() != null && !admin.getEmail().isEmpty()) {
                updateAdmin.setEmail(admin.getEmail());
            }

            AdminEntity savedAdmin = adminRepository.save(updateAdmin);
            return Result.success(savedAdmin);
        } catch (OptimisticLockingFailureException e) {
            return Result.error(StatusCode.FAIL, "数据已被修改，请刷新后重试");
        } catch (ConstraintViolationException e) {
            return Result.error(StatusCode.FAIL, "必需字段不能为空");
        } catch (DataAccessException e) {
            System.err.println("编辑管理员失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "编辑管理员失败");
        }
    }

    // 新增：删除管理员
    @Transactional
    public Result<Void> deleteAdmin(Long id) {
        try {
            if (!adminRepository.existsById(id)) {
                return Result.error(StatusCode.USERNAME_NOT_FOUND, "管理员不存在");
            }

            // 检查是否有学校关联该管理员（可选，根据业务需求）
            List<SchoolEntity> schools = schoolRepository.findByAdminId(id);
            if (!schools.isEmpty()) {
                return Result.error(StatusCode.FAIL, "该管理员关联了校区，请先解除关联");
            }

            adminRepository.deleteById(id);
            return Result.success();
        } catch (DataAccessException e) {
            System.err.println("删除管理员失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "删除管理员失败");
        }
    }


    public Result<String> login(String username, String password) {
        SuperAdminEntity temp = superAdminRepository.findByUsername(username);
        if (temp == null) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "用户名不存在");
        }
        else if (!temp.getPassword().equals(password)) {
            return Result.error(StatusCode.PASSWORD_ERROR, "密码错误");
        }
        return tokenService.createToken(true, false, false, false, temp.getId());
    }
    public Result<SchoolEntity> createSchool(SchoolEntity school) {
        try {
            schoolRepository.save(school);

            for (int i=0;i<school.getTable_num();i++) {
                TableEntity tableEntity = new TableEntity();
                tableEntity.setOccupied(false);
                tableEntity.setSchoolId(school.getId());
                tableRepository.save(tableEntity);
            }

            return Result.success(school);
        } catch (OptimisticLockingFailureException e) {
            System.err.println("数据已被其他用户修改，请刷新后重试");
            return Result.error(StatusCode.FAIL, "数据已被其他用户修改，请刷新后重试");
        } catch (ConstraintViolationException e) {
            System.err.println("必需字段空缺");
            return Result.error(StatusCode.FAIL, "必需字段空缺");
        } catch (DataAccessException e) {
            System.err.println("创建学校失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "创建学校失败");
        }
    }

    // 获取所有学校
    public Result<List<SchoolEntity>> getAllSchools() {
        try {
            List<SchoolEntity> schools = schoolRepository.findAll();
            return Result.success(schools);
        } catch (DataAccessException e) {
            System.err.println("获取学校列表失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "获取学校列表失败");
        }
    }

    @Transactional
    public Result<SchoolEntity> reviseSchool(SchoolEntity school) {
        Optional<SchoolEntity> tempOpt = schoolRepository.findById(school.getId());
        if (tempOpt.isEmpty()) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "学校不存在");
        }
        SchoolEntity temp = tempOpt.get(); // 获取唯一实体

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
        // 注意：需要保存更新后的实体（原代码漏了 save 操作）
        SchoolEntity updated = schoolRepository.save(temp);
        return Result.success(temp);

    }

    // 按ID删除学校
    @Transactional
    public Result<Void> deleteSchool(Long id) {
        try {
            // 检查学校是否存在
            if (!schoolRepository.existsById(id)) {
                return Result.error(StatusCode.USERNAME_NOT_FOUND, "学校不存在");
            }

            // 删除关联的球台
            List<TableEntity> tables = tableRepository.findAllBySchoolId(id);
            tableRepository.deleteAll(tables);

            // 删除学校
            schoolRepository.deleteById(id);
            return Result.success();
        } catch (DataAccessException e) {
            System.err.println("删除学校失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "删除学校失败");
        }
    }

    // 获取所有待审核教练
    public Result<List<CoachEntity>> getAllUncertifiedCoaches() {
        try {
            // 查询所有未审核的教练
            List<CoachEntity> uncertifiedCoaches = coachRepository.findAllByisCertified(false);
            return Result.success(uncertifiedCoaches);
        } catch (DataAccessException e) {
            System.err.println("获取所有未审核教练失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "获取所有未审核教练失败");
        }
    }
    //获取教练信息
    public Result<CoachEntity> getSuperCoachDetail(Long coachId) {
        Optional<CoachEntity> coachOpt = coachRepository.findById(coachId);
        if (coachOpt.isPresent()) {
            CoachEntity coach = coachOpt.get();
            return Result.success(coach);
        } else {
            return Result.error(StatusCode.FAIL, "未找到该教练信息");
        }
    }
    //审核教练
    @Transactional
    public Result<CoachEntity> superCertifyCoach(Long coachId, boolean isAccepted, int level) {
        Optional<CoachEntity> tmp = coachRepository.findById(coachId);
        if (tmp.isPresent()) {
            CoachEntity coach = tmp.get();
            if (isAccepted) {
                coach.setCertified(true);
                coach.setLevel(level);
                return Result.success(coachRepository.save(coach));
            } else {
                coachRepository.delete(coach);
                return Result.success();
            }
        } else {
            return Result.error(StatusCode.FAIL, "该教练不存在");
        }
    }
}
