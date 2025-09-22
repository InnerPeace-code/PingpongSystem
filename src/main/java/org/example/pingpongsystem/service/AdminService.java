package org.example.pingpongsystem.service;

import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolationException;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.repository.*;
import org.example.pingpongsystem.utility.FileUploadUtil;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.example.pingpongsystem.utility.interfaces.InfoAns;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final StudentRepository studentRepository;
    private final CoachAccountService coachAccountService;

    public AdminService(SchoolRepository schoolRepository, TableRepository tableRepository, AdminRepository adminRepository, CoachRepository coachRepository, TokenService tokenService,StudentRepository studentRepository, CoachAccountService coachAccountService) {
        this.schoolRepository = schoolRepository;
        this.tableRepository = tableRepository;
        this.adminRepository = adminRepository;
        this.coachRepository = coachRepository;
        this.tokenService = tokenService;
        this.studentRepository = studentRepository;
        this.coachAccountService = coachAccountService;
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
                coachRepository.save(coach);

                // 审核通过时创建教练账户
                coachAccountService.createCoachAccount(coachId);

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

    @Transactional
    public Result<String> uploadAvatar(Long adminId, MultipartFile file) {
        try {
            Optional<AdminEntity> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                return Result.error(StatusCode.USERNAME_NOT_FOUND, "管理员不存在");
            }
            AdminEntity admin = adminOpt.get();

            Result<String> uploadResult = FileUploadUtil.uploadAvatar(file);
            if (!uploadResult.isSuccess()) {
                return uploadResult;
            }
            admin.setAvatar(uploadResult.getData());
            adminRepository.save(admin);
            return Result.success("头像上传成功");
        } catch (IOException e) {
            return Result.error(StatusCode.FAIL, "头像上传失败：" + e.getMessage());
        }
    }

    @Transactional
    public Result<AdminEntity> updateInfo(InfoAns info) {
        try {
            // 验证用户ID
            if (info.getUserId() == null || !"admin".equals(info.getRole())) {
                return Result.error(StatusCode.FAIL, "无效的管理员信息");
            }

            // 查询管理员是否存在
            Optional<AdminEntity> adminOpt = adminRepository.findById(info.getUserId());
            if (adminOpt.isEmpty()) {
                return Result.error(StatusCode.USERNAME_NOT_FOUND, "管理员不存在");
            }

            AdminEntity admin = adminOpt.get();

            // 只更新AdminEntity中存在的字段（参考AdminEntity定义）
            if (info.getUsername() != null && !info.getUsername().isEmpty()) {
                // 检查用户名是否已存在（排除当前用户自身）
                AdminEntity existing = adminRepository.findByUsername(info.getUsername());
                if (existing != null && !existing.getId().equals(admin.getId())) {
                    return Result.error(StatusCode.FAIL, "用户名已存在");
                }
                admin.setUsername(info.getUsername());
            }

            if (info.getPassword() != null && !info.getPassword().isEmpty()) {
                admin.setPassword(info.getPassword());
            }

            if (info.getName() != null && !info.getName().isEmpty()) {
                admin.setName(info.getName());
            }

            if (info.getPhone() != null && !info.getPhone().isEmpty()) {
                admin.setPhone(info.getPhone());
            }

            if (info.getEmail() != null && !info.getEmail().isEmpty()) {
                admin.setEmail(info.getEmail());
            }

            if (info.getAvatar() != null) {
                admin.setAvatar(info.getAvatar());
            }

            // 保存更新
            AdminEntity updated = adminRepository.save(admin);
            return Result.success(updated);

        } catch (OptimisticLockingFailureException e) {
            return Result.error(StatusCode.FAIL, "数据已被修改，请刷新后重试");
        } catch (DataAccessException e) {
            System.err.println("更新管理员信息失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "更新信息失败");
        }
    }


    public Result<List<CoachEntity>> getCoachesBySchoolId(String token, Long schoolId) {
        // 1. 通过token获取管理员信息
        Result<InfoAns> infoResult = tokenService.getInfo(token);
        if (!infoResult.isSuccess()) {
            return Result.error(StatusCode.FAIL, "获取管理员信息失败：" + infoResult.getMessage());
        }
        InfoAns adminInfo = infoResult.getData();
        if (!"admin".equals(adminInfo.getRole()) && !"super_admin".equals(adminInfo.getRole())) {
            return Result.error(StatusCode.FAIL, "权限不足，非管理员用户");
        }
        Long adminId = Long.valueOf(adminInfo.getUserId());

        // 2. 验证校区是否属于该管理员管辖
        Result<Boolean> checkResult = checkSchoolManagedByAdmin(adminId, schoolId);
        if (!checkResult.isSuccess()) {
            return Result.error(checkResult.getCode(), checkResult.getMessage());
        }

        // 3. 查询该校区的所有教练（假设CoachRepository有findBySchoolId方法）
        List<CoachEntity> coaches = coachRepository.findBySchoolId(schoolId);
        return Result.success(coaches);
    }

    /**
     * 按校区ID获取学生列表（需验证校区是否属于当前管理员管辖）
     */
    public Result<List<StudentEntity>> getStudentsBySchoolId(String token, Long schoolId) {
        // 1. 通过token获取管理员信息
        Result<InfoAns> infoResult = tokenService.getInfo(token);
        if (!infoResult.isSuccess()) {
            return Result.error(StatusCode.FAIL, "获取管理员信息失败：" + infoResult.getMessage());
        }
        InfoAns adminInfo = infoResult.getData();
        if (!"admin".equals(adminInfo.getRole()) && !"super_admin".equals(adminInfo.getRole())) {
            return Result.error(StatusCode.FAIL, "权限不足，非管理员用户");
        }
        Long adminId = Long.valueOf(adminInfo.getUserId());

        // 2. 验证校区是否属于该管理员管辖
        Result<Boolean> checkResult = checkSchoolManagedByAdmin(adminId, schoolId);
        if (!checkResult.isSuccess()) {
            return Result.error(checkResult.getCode(), checkResult.getMessage());
        }

        // 3. 查询该校区的所有学生（假设StudentRepository有findBySchoolId方法）
        List<StudentEntity> students = studentRepository.findBySchoolId(schoolId);
        return Result.success(students);
    }

    /**
     * 校验校区是否由指定管理员管辖
     */
    private Result<Boolean> checkSchoolManagedByAdmin(Long adminId, Long schoolId) {
        // 检查校区是否存在
        Optional<SchoolEntity> schoolOpt = schoolRepository.findById(schoolId);
        if (schoolOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "校区不存在");
        }

        // 检查校区的管理员ID是否与当前管理员一致
        SchoolEntity school = schoolOpt.get();
        if (!school.getAdminId().equals(adminId)) {
            return Result.error(StatusCode.FAIL, "权限不足，该校区不属于您管辖");
        }

        return Result.success(true);
    }

    /**
     * 分页查询所辖校区学生（新增姓名筛选）
     * 新增参数：String name - 学生姓名（模糊筛选，可选）
     */
    public Result<Page<StudentEntity>> getStudentsBySchoolIdWithPage(
            String token,
            Long schoolId,
            String name, // 新增：学生姓名筛选参数
            Integer pageNum,
            Integer pageSize) {
        // 1. 原有逻辑：token验证+权限校验
        Result<InfoAns> infoResult = tokenService.getInfo(token);
        if (!infoResult.isSuccess()) {
            return Result.error(StatusCode.FAIL, "令牌无效：" + infoResult.getMessage());
        }
        InfoAns adminInfo = infoResult.getData();
        if (!"admin".equals(adminInfo.getRole()) && !"super_admin".equals(adminInfo.getRole())) {
            return Result.error(StatusCode.FAIL, "权限不足，非管理员");
        }
        Long adminId = Long.valueOf(adminInfo.getUserId());

        // 2. 原有逻辑：获取管理员所辖校区ID列表
        List<SchoolEntity> managedSchools = schoolRepository.findByAdminId(adminId);
        if (managedSchools.isEmpty()) {
            return Result.success(Page.empty());
        }
        List<Long> managedSchoolIds = managedSchools.stream()
                .map(SchoolEntity::getId)
                .collect(Collectors.toList());

        // 3. 原有逻辑：校验schoolId权限（若指定校区）
        if (schoolId != null && !managedSchoolIds.contains(schoolId)) {
            return Result.error(StatusCode.FAIL, "无权限访问该校区");
        }

        // 4. 分页参数（页码从0开始，前端传1对应第0页）
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);

        // 5. 新增核心：动态构建筛选条件（校区+姓名）
        Specification<StudentEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 条件1：校区筛选（指定校区/所辖所有校区）
            if (schoolId != null) {
                // 指定单个校区：schoolId = 传入值
                predicates.add(cb.equal(root.get("schoolId"), schoolId));
            } else {
                // 未指定校区：schoolId 在管理员所辖校区列表中
                predicates.add(root.get("schoolId").in(managedSchoolIds));
            }

            // 条件2：姓名模糊筛选（仅当name非空且去空格后不为空）
            if (name != null && !name.trim().isEmpty()) {
                String fuzzyName = "%" + name.trim() + "%"; // 模糊匹配格式：%姓名%
                predicates.add(cb.like(root.get("name"), fuzzyName));
            }

            // 组合所有条件
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 6. 执行分页查询（使用动态条件）
        Page<StudentEntity> students = studentRepository.findAll(spec, pageable);

        return Result.success(students);
    }


    /**
     * 分页查询所辖校区已认证教练（新增姓名+等级筛选）
     * 新增参数：String name - 教练姓名（模糊筛选）、Integer level - 教练等级（精确筛选）
     */
    public Result<Page<CoachEntity>> getCertifiedCoachesBySchoolIdWithPage(
            String token,
            Long schoolId,
            String name, // 新增：教练姓名筛选
            Integer level, // 新增：教练等级筛选（10/100/1000）
            Integer pageNum,
            Integer pageSize) {
        // 1. 原有逻辑：token验证+权限校验
        Result<InfoAns> infoResult = tokenService.getInfo(token);
        if (!infoResult.isSuccess()) {
            return Result.error(StatusCode.FAIL, "令牌无效：" + infoResult.getMessage());
        }
        InfoAns adminInfo = infoResult.getData();
        if (!"admin".equals(adminInfo.getRole()) && !"super_admin".equals(adminInfo.getRole())) {
            return Result.error(StatusCode.FAIL, "权限不足，非管理员");
        }
        Long adminId = Long.valueOf(adminInfo.getUserId());

        // 2. 原有逻辑：获取管理员所辖校区ID列表
        List<SchoolEntity> managedSchools = schoolRepository.findByAdminId(adminId);
        if (managedSchools.isEmpty()) {
            return Result.success(Page.empty());
        }
        List<Long> managedSchoolIds = managedSchools.stream()
                .map(SchoolEntity::getId)
                .collect(Collectors.toList());

        // 3. 原有逻辑：校验schoolId权限（若指定校区）
        if (schoolId != null && !managedSchoolIds.contains(schoolId)) {
            return Result.error(StatusCode.FAIL, "无权限访问该校区");
        }

        // 4. 分页参数（页码从0开始）
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);

        // 5. 新增核心：动态构建筛选条件（校区+已认证+姓名+等级）
        Specification<CoachEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 条件1：固定筛选「已认证教练」（接口语义要求）
            predicates.add(cb.equal(root.get("isCertified"), true));

            // 条件2：校区筛选（指定校区/所辖所有校区）
            if (schoolId != null) {
                predicates.add(cb.equal(root.get("schoolId"), schoolId));
            } else {
                predicates.add(root.get("schoolId").in(managedSchoolIds));
            }

            // 条件3：姓名模糊筛选（仅当name非空且去空格后不为空）
            if (name != null && !name.trim().isEmpty()) {
                String fuzzyName = "%" + name.trim() + "%";
                predicates.add(cb.like(root.get("name"), fuzzyName));
            }

            // 条件4：等级精确筛选（仅当level为合法值：10/100/1000）
            if (level != null && (level == 10 || level == 100 || level == 1000)) {
                predicates.add(cb.equal(root.get("level"), level));
            }

            // 组合所有条件
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 6. 执行分页查询（使用动态条件）
        Page<CoachEntity> coaches = coachRepository.findAll(spec, pageable);

        return Result.success(coaches);
    }

    @Transactional
    public Result<CoachEntity> updateCertifiedCoach(String token, CoachEntity updatedCoach) {
        // 1. 验证管理员权限
        Result<InfoAns> infoResult = tokenService.getInfo(token);
        if (!infoResult.isSuccess()) {
            return Result.error(StatusCode.FAIL, "令牌无效：" + infoResult.getMessage());
        }
        InfoAns adminInfo = infoResult.getData();
        if (!"admin".equals(adminInfo.getRole()) && !"super_admin".equals(adminInfo.getRole())) {
            return Result.error(StatusCode.FAIL, "权限不足，非管理员用户");
        }
        Long adminId = Long.valueOf(adminInfo.getUserId());

        // 2. 检查教练是否存在
        Long coachId = updatedCoach.getId();
        if (coachId == null) {
            return Result.error(StatusCode.FAIL, "教练ID不能为空");
        }
        Optional<CoachEntity> coachOpt = coachRepository.findById(coachId);
        if (coachOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "未找到该教练");
        }
        CoachEntity coach = coachOpt.get();

        // 3. 检查教练是否已认证（仅允许更新已认证教练）
        if (!coach.isCertified()) {
            return Result.error(StatusCode.FAIL, "仅允许更新已认证教练的信息");
        }

        // 4. 验证教练所属校区是否由当前管理员管辖
        Result<Boolean> checkResult = checkSchoolManagedByAdmin(adminId, coach.getSchoolId());
        if (!checkResult.isSuccess()) {
            return Result.error(checkResult.getCode(), checkResult.getMessage());
        }

        // 5. 更新教练信息（按需更新可修改字段）
        if (updatedCoach.getName() != null && !updatedCoach.getName().isEmpty()) {
            coach.setName(updatedCoach.getName());
        }
        if (updatedCoach.getPhone() != null && !updatedCoach.getPhone().isEmpty()) {
            coach.setPhone(updatedCoach.getPhone());
        }
        if (updatedCoach.getEmail() != null && !updatedCoach.getEmail().isEmpty()) {
            coach.setEmail(updatedCoach.getEmail());
        }
        if (updatedCoach.getAge() > 0 && updatedCoach.getAge() < 200) {
            coach.setAge(updatedCoach.getAge());
        }
        if (updatedCoach.getLevel() == 10 || updatedCoach.getLevel() == 100 || updatedCoach.getLevel() == 1000) {
            coach.setLevel(updatedCoach.getLevel());
        }
        if (updatedCoach.getDescription() != null) {
            coach.setDescription(updatedCoach.getDescription());
        }
        // 注意：如果更新照片或核心信息，可能需要重新审核（根据业务需求调整）
        coach.setIsMale(updatedCoach.getIsMale());
        // 6. 保存更新
        CoachEntity savedCoach = coachRepository.save(coach);
        return Result.success(savedCoach);
    }

    /**
     * 更新学生信息（管理员权限）
     */
    @Transactional
    public Result<StudentEntity> updateStudent(String token, StudentEntity updatedStudent) {
        // 1. 验证管理员权限
        Result<InfoAns> infoResult = tokenService.getInfo(token);
        if (!infoResult.isSuccess()) {
            return Result.error(StatusCode.FAIL, "令牌无效：" + infoResult.getMessage());
        }
        InfoAns adminInfo = infoResult.getData();
        if (!"admin".equals(adminInfo.getRole()) && !"super_admin".equals(adminInfo.getRole())) {
            return Result.error(StatusCode.FAIL, "权限不足，非管理员用户");
        }
        Long adminId = Long.valueOf(adminInfo.getUserId());

        // 2. 检查学生是否存在
        Long studentId = updatedStudent.getId();
        if (studentId == null) {
            return Result.error(StatusCode.FAIL, "学生ID不能为空");
        }
        Optional<StudentEntity> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "未找到该学生");
        }
        StudentEntity student = studentOpt.get();

        // 3. 验证学生所属校区是否由当前管理员管辖
        Result<Boolean> checkResult = checkSchoolManagedByAdmin(adminId, student.getSchoolId());
        if (!checkResult.isSuccess()) {
            return Result.error(checkResult.getCode(), checkResult.getMessage());
        }

        // 4. 更新学生信息（按需更新可修改字段）
        if (updatedStudent.getName() != null && !updatedStudent.getName().isEmpty()) {
            student.setName(updatedStudent.getName());
        }
        if (updatedStudent.getPhone() != null && !updatedStudent.getPhone().isEmpty()) {
            student.setPhone(updatedStudent.getPhone());
        }
        if (updatedStudent.getEmail() != null && !updatedStudent.getEmail().isEmpty()) {
            student.setEmail(updatedStudent.getEmail());
        }
        if (updatedStudent.getAge() != null && updatedStudent.getAge() > 0 && updatedStudent.getAge() < 200) {
            student.setAge(updatedStudent.getAge());
        }
        if (updatedStudent.isMale() != student.isMale()) {
            student.setMale(updatedStudent.isMale());
        }
        // 注意：如果允许修改校区，需要额外校验新校区是否属于该管理员管辖
        if (updatedStudent.getSchoolId() != null && !updatedStudent.getSchoolId().equals(student.getSchoolId())) {
            Result<Boolean> newSchoolCheck = checkSchoolManagedByAdmin(adminId, updatedStudent.getSchoolId());
            if (newSchoolCheck.isSuccess()) {
                student.setSchoolId(updatedStudent.getSchoolId());
            } else {
                return Result.error(newSchoolCheck.getCode(), "无权将学生转移到该校区：" + newSchoolCheck.getMessage());
            }
        }

        // 5. 保存更新
        StudentEntity savedStudent = studentRepository.save(student);
        return Result.success(savedStudent);
    }
}
