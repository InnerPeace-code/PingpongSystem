package org.example.pingpongsystem.service;

import jakarta.validation.ConstraintViolationException;
import org.example.pingpongsystem.entity.CoachEntity;
import org.example.pingpongsystem.entity.CoachTeachStudentEntity;
import org.example.pingpongsystem.repository.CoachRepository;
import org.example.pingpongsystem.repository.CoachTeachStudentRepository;
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

@Service
public class CoachService {
    private final CoachRepository coachRepository;
    private final CoachTeachStudentRepository coachTeachStudentRepository;
    private final TokenService tokenService;

    public CoachService(CoachRepository coachRepository, CoachTeachStudentRepository coachTeachStudentRepository, TokenService tokenService) {
        this.coachRepository = coachRepository;  // 由Spring容器注入实例
        this.coachTeachStudentRepository = coachTeachStudentRepository;
        this.tokenService = tokenService;
    }

    public Result<String> save(CoachEntity coach, MultipartFile file) {
        try {
            coach.setId(null);
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
    public Result<CoachEntity> revise(CoachEntity coach, MultipartFile file) {
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
            if (coach.isMale() != temp.isMale()) {
                temp.setMale(coach.isMale());
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
            if (!file.isEmpty()) {
                try {
                    Result<String> result = savePhoto(file);
                    if (!result.isSuccess()) return Result.error(StatusCode.FAIL, result.getMessage());

                    Path path = Paths.get(coach.getPhotoPath());
                    Files.delete(path);

                    temp.setPhotoPath(result.getData());
                    temp.setCertified(false);
                } catch (IOException e) {
                    System.err.println("IO失败: " + e.getMessage());
                    e.printStackTrace();
                    return Result.error(StatusCode.FAIL, "IO失败");
                }
            }
            if (!coach.getDescription().equals(temp.getDescription())) {
                if (!coach.getDescription().isEmpty()) {
                    temp.setDescription(coach.getDescription());
                    temp.setCertified(false);
                }
            }
            return Result.success(temp);
        }
    }

    public Result<List<CoachEntity>> getAll() {
        return Result.success(coachRepository.findAllByisCertified(true));
    }

    public Result<List<CoachEntity>> getSearched(String name, Boolean isMale, Integer age_low, Integer age_high, Integer level) {
        List<CoachEntity> li = coachRepository.findAllByisCertified(true);
        if (name != null && !name.isEmpty()) {
            List<CoachEntity> li1 = new ArrayList<>();
            for (CoachEntity coach : li) {
                if (coach.getName().contains(name)) {
                    li1.add(coach);
                }
            }
            li = li1;
        }
        if (isMale != null) {
            List<CoachEntity> li2 = new ArrayList<>();
            for (CoachEntity coach : li) {
                if (coach.isMale() == isMale) {
                    li2.add(coach);
                }
            }
            li = li2;
        }
        if (age_low != null) {
            List<CoachEntity> li3 = new ArrayList<>();
            for (CoachEntity coach : li) {
                if (coach.getAge() >= age_low) {
                    li3.add(coach);
                }
            }
            li = li3;
        }
        if (age_high != null) {
            List<CoachEntity> li4 = new ArrayList<>();
            for (CoachEntity coach : li) {
                if (coach.getAge() <= age_high) {
                    li4.add(coach);
                }
            }
            li = li4;
        }
        if (level != null) {
            List<CoachEntity> li5 = new ArrayList<>();
            for (CoachEntity coach : li) {
                if (coach.getLevel() == level) {
                    li5.add(coach);
                }
            }
            li = li5;
        }
        return Result.success(li);
    }

    public Result<List<CoachTeachStudentEntity>> getStudentSelect(Long coachId) {
        List<CoachTeachStudentEntity> li = coachTeachStudentRepository.findByCoachId(coachId);
        List<CoachTeachStudentEntity> li1 = new ArrayList<>();
        for (CoachTeachStudentEntity coachTeachStudentEntity : li) {
            if (!coachTeachStudentEntity.isConfirmed())
                li1.add(coachTeachStudentEntity);
        }
        return Result.success(li1);
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
}
