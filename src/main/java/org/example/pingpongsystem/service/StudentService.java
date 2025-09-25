package org.example.pingpongsystem.service;

import jakarta.validation.ConstraintViolationException;
import org.example.pingpongsystem.entity.CoachEntity;
import org.example.pingpongsystem.entity.CoachTeachStudentEntity;
import org.example.pingpongsystem.entity.StudentAccountEntity;
import org.example.pingpongsystem.entity.StudentEntity;
import org.example.pingpongsystem.repository.CoachRepository;
import org.example.pingpongsystem.repository.CoachTeachStudentRepository;
import org.example.pingpongsystem.repository.StudentAccountRepository;
import org.example.pingpongsystem.repository.StudentRepository;
import org.example.pingpongsystem.utility.FileUploadUtil;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentService {
    private final StudentRepository studentRepository;
    private final CoachTeachStudentRepository coachTeachStudentRepository;
    private final CoachRepository coachRepository;
    private final TokenService tokenService;
    private final StudentAccountRepository studentAccountRepository;

    public StudentService(StudentRepository studentRepository, CoachTeachStudentRepository coachTeachStudentRepository, CoachRepository coachRepository, TokenService tokenService,StudentAccountRepository studentAccountRepository) {
        this.studentRepository = studentRepository;  // 由Spring容器注入实例
        this.coachTeachStudentRepository = coachTeachStudentRepository;
        this.coachRepository = coachRepository;
        this.tokenService = tokenService;
        this.studentAccountRepository = studentAccountRepository;
    }

    public Result<String> save(StudentEntity student) {
        // 新增：检查用户名是否已存在
        StudentEntity existing = studentRepository.findByUsername(student.getUsername());
        if (existing != null) {
            System.err.println("用户名已存在");
            return Result.error(StatusCode.FAIL,"用户名已存在");
        }

        if (student.getAge() == null) {
            student.setAge(0);
        }
        try {
            // 保存学生信息（会自动生成学生ID）
            StudentEntity savedStudent = studentRepository.save(student);
            // 新增：创建学生账户
            StudentAccountEntity account = new StudentAccountEntity();
            account.setStudentId(savedStudent.getId());  // 关联学生ID
            account.setBalance(0.0);  // 初始余额为0
            studentAccountRepository.save(account);  // 保存账户
            return Result.success();
        } catch (OptimisticLockingFailureException e) {
            System.err.println("数据已被其他用户修改，请刷新后重试");
            return Result.error(StatusCode.FAIL,"数据已被其他用户修改，请刷新后重试");
        } catch (ConstraintViolationException e) {
            System.err.println("必需字段空缺");
            return Result.error(StatusCode.FAIL,"用户名、密码、校区、姓名和电话是必须填写的信息，其他信息可空白");
        } catch (DataAccessException e) {
            System.err.println("保存学生信息失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL,"保存学生信息失败");
        }
    }

    public Result<String> login(String username, String password) {
        StudentEntity temp = studentRepository.findByUsername(username);
        if (temp == null) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "用户名不存在");
        }
        else if (!temp.getPassword().equals(password)) {
            return Result.error(StatusCode.PASSWORD_ERROR, "密码错误");
        }
        return tokenService.createToken(false, false, false, true, temp.getId());
    }

    @Transactional
    public Result<StudentEntity> revise(StudentEntity student) {
        StudentEntity temp = studentRepository.findByUsername(student.getUsername());
        if (temp == null) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "用户名不存在");
        }
        else {
            if (!student.getPassword().equals(temp.getPassword())) {
                if (!student.getPassword().isEmpty())
                    temp.setPassword(student.getPassword());
            }
            if (!student.getName().equals(temp.getName())) {
                if (!student.getName().isEmpty())
                    temp.setName(student.getName());
            }
            if (student.isMale() != temp.isMale()) {
                temp.setMale(student.isMale());
            }
            if (student.getAge() != temp.getAge()) {
                if (student.getAge() > 0 && student.getAge() < 200)
                    temp.setAge(student.getAge());
            }
            if (!student.getPhone().equals(temp.getPhone())) {
                if (!student.getPhone().isEmpty())
                    temp.setPhone(student.getPhone());
            }
            if (!student.getEmail().equals(temp.getEmail())) {
                if (!student.getEmail().isEmpty())
                    temp.setEmail(student.getEmail());
            }
            if(student.getSchoolId() != temp.getId()){
                temp.setSchoolId(student.getSchoolId());
            }
            return Result.success(temp);
        }
    }

//    @Transactional
//    public Result<CoachTeachStudentEntity> selectCoach(Long coachId, Long studentId) {
//        Optional<CoachEntity> tmp = coachRepository.findById(coachId);
//        if (tmp.isPresent()) {
//            CoachEntity coach = tmp.get();
//            if (coachTeachStudentRepository.countByCoachId(coachId) >= 20) {
//                return Result.error(StatusCode.FAIL, "该教练暂时没有名额");
//            }
//            else if (coachTeachStudentRepository.countByStudentId(studentId) >= 2) {
//                return Result.error(StatusCode.FAIL, "学员的教练数量已达上限（2个）");
//            }
//            else {
//                Optional<CoachEntity> t = coachRepository.findById(coachId);
//                if (t.isPresent()) {
//                    CoachTeachStudentEntity coachTeachStudentEntity = new CoachTeachStudentEntity();
//                    coachTeachStudentEntity.setCoachId(coachId);
//                    coachTeachStudentEntity.setStudentId(studentId);
//                    coachTeachStudentEntity.setConfirmed(false);
//                    coachTeachStudentRepository.save(coachTeachStudentEntity);
//                    return Result.success(coachTeachStudentEntity);
//                }
//                else return Result.error(StatusCode.FAIL, "学员不存在");
//            }
//        }
//        else {
//            return Result.error(StatusCode.FAIL, "未找到该教练");
//        }
//    }
    @Transactional
    public Result<CoachTeachStudentEntity> selectCoach(Long coachId, Long studentId) {
        // 1. 检查教练是否存在
        Optional<CoachEntity> coachOpt = coachRepository.findById(coachId);
        if (!coachOpt.isPresent()) {
            return Result.error(StatusCode.FAIL, "未找到该教练");
        }

        // 2. 新增：检查学员是否已申请过该教练（核心逻辑）
        // 用 coachId + studentId 联合查询，确认是否存在关联记录
        Optional<CoachTeachStudentEntity> existingRelation =
                coachTeachStudentRepository.findByCoachIdAndStudentId(coachId, studentId);

        if (existingRelation.isPresent()) {
            CoachTeachStudentEntity relation = existingRelation.get();
            // 根据 confirmed 状态返回不同提示
            if (relation.isConfirmed()) {
                // 已确认：说明已是该教练学生
                return Result.error(StatusCode.FAIL, "您已成为该教练的学员，无需重复申请");
            } else {
                // 未确认：说明申请已提交，等待审核
                return Result.error(StatusCode.FAIL, "您已提交该教练的申请，正在等待教练审核");
            }
        }

        // 3. 检查教练名额是否已满（原逻辑保留）
        if (coachTeachStudentRepository.countByCoachId(coachId) >= 20) {
            return Result.error(StatusCode.FAIL, "该教练暂时没有名额");
        }

        // 4. 检查学员已选教练数量是否达上限（原逻辑保留）
        // 注意：这里统计的是“所有关联记录”（包括未确认的），若需仅统计“已确认”，需修改SQL
        if (coachTeachStudentRepository.countByStudentId(studentId) >= 2) {
            return Result.error(StatusCode.FAIL, "学员的教练数量已达上限（2个）");
        }

        // 5. 无重复申请，创建新的关联记录（原逻辑保留）
        CoachTeachStudentEntity coachTeachStudentEntity = new CoachTeachStudentEntity();
        coachTeachStudentEntity.setCoachId(coachId);
        coachTeachStudentEntity.setStudentId(studentId);
        coachTeachStudentEntity.setConfirmed(false); // 初始状态：未确认
        coachTeachStudentRepository.save(coachTeachStudentEntity);

        return Result.success(coachTeachStudentEntity);
    }

    @Transactional
    public Result<String> uploadAvatar(Long studentId, MultipartFile file) {
        try {
            Optional<StudentEntity> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                return Result.error(StatusCode.USERNAME_NOT_FOUND, "学员不存在");
            }
            StudentEntity student = studentOpt.get();

            Result<String> uploadResult = FileUploadUtil.uploadAvatar(file);
            if (!uploadResult.isSuccess()) {
                return uploadResult;
            }
            student.setAvatar(uploadResult.getData());
            studentRepository.save(student);
            return Result.success("头像上传成功");
        } catch (IOException e) {
            return Result.error(StatusCode.FAIL, "头像上传失败：" + e.getMessage());
        }
    }

    public Result<CoachEntity> getStudentCoachDetail(Long coachId) {
        // 1. 根据ID查询教练
        Optional<CoachEntity> coachOpt = coachRepository.findById(coachId);
        if (!coachOpt.isPresent()) {
            return Result.error(StatusCode.FAIL, "未找到该教练信息");
        }

        CoachEntity coach = coachOpt.get();
        // 2. 权限控制：学生只能查看“已审核通过”的教练（与管理员逻辑相反）
        if (coach.isCertified()) {
            return Result.success(coach); // 返回已审核教练的完整详情
        } else {
            return Result.error(StatusCode.FAIL, "该教练暂未通过审核，无法查看详情");
        }
    }

    // StudentService.java
    public Result<List<CoachEntity>> getRelatedCoaches(Long studentId) {
        try {
            // 1. 查询该学员已确认的教练关系
            List<CoachTeachStudentEntity> relations = coachTeachStudentRepository
                    .findByStudentIdAndIsConfirmed(studentId, true);

            // 2. 提取教练ID列表
            List<Long> coachIds = relations.stream()
                    .map(CoachTeachStudentEntity::getCoachId)
                    .collect(Collectors.toList());

            // 3. 查询对应的教练信息
            List<CoachEntity> coaches = coachRepository.findAllById(coachIds);

            return Result.success(coaches);
        } catch (DataAccessException e) {
            System.err.println("获取相关教练失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "获取相关教练失败");
        }
    }

    public Result<StudentEntity> getStudentById(Long studentId) {
        // 1. 参数校验
        if (studentId == null) {
            return Result.error(StatusCode.FAIL, "学生ID不能为空");
        }

        // 2. 查询学生信息
        Optional<StudentEntity> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isPresent()) {
            return Result.success(studentOpt.get()); // 返回学生基础信息（姓名、学号、校区等）
        } else {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "未找到该学生信息");
        }
    }
}
