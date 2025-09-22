package org.example.pingpongsystem.service;

import jakarta.validation.ConstraintViolationException;
import org.example.pingpongsystem.dto.CoachStudentDTO;
import org.example.pingpongsystem.entity.CoachEntity;
import org.example.pingpongsystem.entity.CoachTeachStudentEntity;
import org.example.pingpongsystem.entity.StudentEntity;
import org.example.pingpongsystem.repository.CoachRepository;
import org.example.pingpongsystem.repository.CoachTeachStudentRepository;
import org.example.pingpongsystem.repository.StudentRepository;
import org.example.pingpongsystem.utility.FileUploadUtil;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.example.pingpongsystem.utility.Utility;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CoachService {
    private final CoachRepository coachRepository;
    private final CoachTeachStudentRepository coachTeachStudentRepository;
    private final TokenService tokenService;
    private final StudentRepository studentRepository;

    public CoachService(CoachRepository coachRepository, CoachTeachStudentRepository coachTeachStudentRepository, TokenService tokenService, StudentRepository studentRepository) {
        this.coachRepository = coachRepository;  // 由Spring容器注入实例
        this.coachTeachStudentRepository = coachTeachStudentRepository;
        this.tokenService = tokenService;
        this.studentRepository = studentRepository;
    }

    public Result<String> save(CoachEntity coach, MultipartFile file) {
        try {
            CoachEntity existing = coachRepository.findByUsername(coach.getUsername());
            if (existing != null) {
                return Result.error(StatusCode.FAIL, "用户名已存在");
            }

            coach.setId(null);//为什么？
            coach.setCertified(false);
            coach.setVersion(null);

            Result<String> result = savePhoto(file);
            if (!result.isSuccess()) return result;
            coach.setPhotoPath(result.getData());

            coachRepository.save(coach);
            return Result.success();
        } catch (OptimisticLockingFailureException e) {
            System.err.println("数据已被其他用户修改，请刷新后重试");
            return Result.error(StatusCode.FAIL, "数据已被其他用户修改，请刷新后重试");
        } catch (ConstraintViolationException e) {
            System.err.println("必需字段空缺");
            return Result.error(StatusCode.FAIL, "必需字段空缺");
        } catch (DataAccessException e) {
            System.err.println("保存教练信息失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "保存教练信息失败");
        } catch (IOException e) {
            System.err.println("IO失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error(StatusCode.FAIL, "IO失败");
        }
    }

    public Result<String> login(String username, String password) {
        CoachEntity temp = coachRepository.findByUsername(username);
        if (temp == null) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "用户名不存在");
        }
        else if (!temp.getPassword().equals(password)) {
            return Result.error(StatusCode.PASSWORD_ERROR, "密码错误");
        }// 新增审核状态检查
        else if (!temp.isCertified()) {
            return Result.error(StatusCode.FAIL, "账号待审核，请等待管理员审核通过后再登录");
        }
        return tokenService.createToken(false, false, true, false, temp.getId());
    }

    @Transactional
    public Result<CoachEntity> revise(CoachEntity coach) {
        CoachEntity temp = coachRepository.findByUsername(coach.getUsername());
        if (temp == null) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "用户名不存在");
        }
        else {
            if (!coach.getPassword().equals(temp.getPassword())) {
                if (!coach.getPassword().isEmpty())
                    temp.setPassword(coach.getPassword());
            }
            if (!coach.getName().equals(temp.getName())) {
                if (!coach.getName().isEmpty())
                    temp.setName(coach.getName());
            }
            if (coach.getIsMale() != temp.getIsMale()) {
                temp.setIsMale(coach.getIsMale());
            }
            if (coach.getAge() != temp.getAge()) {
                if (coach.getAge() > 0 && coach.getAge() < 200)
                    temp.setAge(coach.getAge());
            }
            if (!coach.getPhone().equals(temp.getPhone())) {
                if (!coach.getPhone().isEmpty())
                    temp.setPhone(coach.getPhone());
            }
            if (!coach.getEmail().equals(temp.getEmail())) {
                if (!coach.getEmail().isEmpty())
                    temp.setEmail(coach.getEmail());
            }

            if (!coach.getDescription().equals(temp.getDescription())) {
                if (!coach.getDescription().isEmpty()) {
                    temp.setDescription(coach.getDescription());
                    //temp.setCertified(false);
                }
            }

            temp.setAvatar(coach.getAvatar());  // 假设CoachEntity新增了avatar字段

            if(coach.getPhotoPath().isEmpty()){
                return Result.error(StatusCode.FAIL,"照片不能为空");
            }
            // 新增：如果传入了教练照片路径，更新照片（原逻辑保留，这里兼容前端分离上传）
            if (coach.getPhotoPath() != null && !coach.getPhotoPath().isEmpty()) {
                temp.setPhotoPath(coach.getPhotoPath());
                //temp.setCertified(false);  // 照片变更需重新审核
            }

            return Result.success(temp);
        }
    }

    public Result<List<CoachEntity>> getAll() {
        return Result.success(coachRepository.findAllByisCertified(true));
    }

    public Result<List<CoachEntity>> getSearched(String name, Boolean isMale, Integer age_low, Integer age_high, Integer level, Long schoolId) {
        List<CoachEntity> li = coachRepository.findAllByisCertified(true);
        // 1. 校区过滤（关键新增）
        if (schoolId != null) {
            li = li.stream()
                    .filter(coach -> schoolId.equals(coach.getSchoolId()))
                    .collect(Collectors.toList());
        }
        if (name != null && !name.isEmpty()) {
            li = li.stream()
                    .filter(coach -> coach.getName().contains(name))
                    .collect(Collectors.toList());
        }
        if (isMale != null) {
            li = li.stream()
                    .filter(coach -> isMale.equals(coach.getIsMale()))
                    .collect(Collectors.toList());
        }
        if (age_low != null) {
            li = li.stream()
                    .filter(coach -> coach.getAge() >= age_low)
                    .collect(Collectors.toList());
        }
        if (age_high != null) {
            li = li.stream()
                    .filter(coach -> coach.getAge() <= age_high)
                    .collect(Collectors.toList());
        }
        if (level != null) {
            li = li.stream()
                    .filter(coach -> level.equals(coach.getLevel()))
                    .collect(Collectors.toList());
        }

        return Result.success(li);
    }

    public Result<List<CoachStudentDTO>> getStudentSelect(Long coachId) {
        List<CoachTeachStudentEntity> relations = coachTeachStudentRepository.findByCoachId(coachId);
        List<CoachStudentDTO> result = new ArrayList<>();

        for (CoachTeachStudentEntity relation : relations) {
            if (!relation.isConfirmed()) { // 只处理未确认的申请
                CoachStudentDTO dto = new CoachStudentDTO();
                dto.setRelation(relation);

                // 查询学生信息
                Optional<StudentEntity> studentOpt = studentRepository.findById(relation.getStudentId());
                studentOpt.ifPresent(dto::setStudent); // 如果找到学生则设置

                result.add(dto);
            }
        }
        return Result.success(result);
    }

    @Transactional
    public Result<CoachTeachStudentEntity> reviewStudentSelect(Long coachTeachStudentId, boolean isAccepted) {
        Optional<CoachTeachStudentEntity> tmp = coachTeachStudentRepository.findById(coachTeachStudentId);
        if (tmp.isPresent()) {
            CoachTeachStudentEntity coachTeachStudentEntity = tmp.get();
            if (isAccepted) {
                coachTeachStudentEntity.setConfirmed(true);
                return Result.success(coachTeachStudentEntity);
            }
            else {
                coachTeachStudentRepository.delete(coachTeachStudentEntity);
                return Result.success();
            }
        }
        else {
            return Result.error(StatusCode.FAIL, "未找到这条学员申请教练记录");
        }
    }

    private Result<String> savePhoto(MultipartFile file) throws IOException {
        // 1. 验证文件（保持不变）
        if (file.isEmpty()) {
            return Result.error(StatusCode.FAIL, "上传失败：文件为空");
        }
        if (!Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
            return Result.error(StatusCode.FAIL, "上传失败：请上传图片文件");
        }

        // 2. 生成唯一文件名（保持不变）
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID() + extension; // 只存文件名，如 "a1b2c3.png"

        // 3. 确保上传目录存在（保持不变）
        Path uploadPath = Paths.get(Utility.CoachPhotoPath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 4. 保存图片到文件系统（保持不变）
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // 5. 只返回文件名（而非完整路径）
        return Result.success(fileName); // 数据库中存储的是 "a1b2c3.png"
    }

//    private Result<String> savePhoto(MultipartFile file) throws IOException {
//        // 1. 验证文件
//        if (file.isEmpty()) {
//            return Result.error(StatusCode.FAIL, "上传失败：文件为空");
//        }
//        if (!Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
//            return Result.error(StatusCode.FAIL, "上传失败：请上传图片文件");
//        }
////            if (file.getSize() > 5 * 1024 * 1024) { // 限制5MB
////                return "上传失败：文件大小不能超过5MB";
////            }
//
//        // 2. 生成唯一文件名（避免冲突）
//        String originalFilename = file.getOriginalFilename();
//        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
//        String fileName = UUID.randomUUID() + extension;
//
//        // 3. 确保上传目录存在
//        Path uploadPath = Paths.get(Utility.CoachPhotoPath);
//        if (!Files.exists(uploadPath)) {
//            Files.createDirectories(uploadPath);
//        }
//
//        // 4. 保存图片到文件系统
//        Path filePath = uploadPath.resolve(fileName);
//        Files.write(filePath, file.getBytes());
//
//        // 5. 保存路径到数据库
//        return Result.success(filePath.toString());
//    }

    @Transactional
    public Result<String> uploadAvatar(Long coachId, MultipartFile file) {
        try {
            Optional<CoachEntity> coachOpt = coachRepository.findById(coachId);
            if (coachOpt.isEmpty()) {
                return Result.error(StatusCode.USERNAME_NOT_FOUND, "教练不存在");
            }
            CoachEntity coach = coachOpt.get();

            // 上传头像并更新路径
            Result<String> uploadResult = FileUploadUtil.uploadAvatar(file);
            if (!uploadResult.isSuccess()) {
                return uploadResult;
            }
            coach.setAvatar(uploadResult.getData());
            coachRepository.save(coach);
            return Result.success("头像上传成功");
        } catch (IOException e) {
            return Result.error(StatusCode.FAIL, "头像上传失败：" + e.getMessage());
        }
    }

    // CoachService.java
    public Result<List<StudentEntity>> getRelatedStudents(Long coachId) {
        try {
            // 1. 查询该教练已确认的学员关系（与学员查教练逻辑对称）
            List<CoachTeachStudentEntity> relations = coachTeachStudentRepository
                    .findByCoachIdAndIsConfirmed(coachId, true);

            // 2. 提取学员ID列表
            List<Long> studentIds = relations.stream()
                    .map(CoachTeachStudentEntity::getStudentId)  // 注意这里是获取学员ID
                    .collect(Collectors.toList());

            // 3. 查询对应的学员信息
            List<StudentEntity> students = studentRepository.findAllById(studentIds);

            return Result.success(students);
        } catch (DataAccessException e) {
            System.err.println("获取相关学员失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "获取相关学员失败");
        }
    }


    public Result<StudentEntity> getCoachStudentDetail(Long studentId) {
        // 1. 根据ID查询教练
        Optional<StudentEntity> studentOpt = studentRepository.findById(studentId);
        if (!studentOpt.isPresent()) {
            return Result.error(StatusCode.FAIL, "未找到该学生信息");
        }
        StudentEntity student = studentOpt.get();
        return Result.success(student);

    }
}
