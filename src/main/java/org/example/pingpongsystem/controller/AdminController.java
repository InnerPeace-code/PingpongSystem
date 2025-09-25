package org.example.pingpongsystem.controller;

import lombok.Data;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.service.AdminService;
import org.example.pingpongsystem.service.CoachService;
import org.example.pingpongsystem.service.PaymentService;
import org.example.pingpongsystem.service.SuperAdminService;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.interfaces.InfoAns;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/admin")
@RestController
public class AdminController {
    private final AdminService adminService;
    private final PaymentService paymentService;
    public AdminController(AdminService adminService, PaymentService paymentService) {
        this.adminService = adminService;
        this.paymentService = paymentService;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody SuperAdminController.LoginRequest request) {
        return adminService.login(request.getUsername(), request.getPassword());
    }

    @GetMapping("/get_coach_register")
    public Result<List<CoachEntity>> getCoachRegister(@RequestParam(name = "token") String token) {
        // 通过token获取当前登录管理员的ID，再查询其管理的所有校区的待审核教练
        return adminService.getUncertifiedCoachesByAdminToken(token);
    }

    @GetMapping("/get_coach_detail")
    public Result<CoachEntity> getCoachDetail(@RequestParam Long coachId) {
        return adminService.getCoachDetail(coachId);
    }

    @PostMapping("/certify_coach")
    public Result<CoachEntity> certifyCoach(@RequestBody CertifyRequest request) {
        System.out.println(request.coachId);
        System.out.println(request.isAccepted);
        return adminService.certifyCoach(request.coachId, request.isAccepted,request.level);
    }

    @Data
    public static class CertifyRequest {
        private Long coachId;
        private Boolean isAccepted;
        private int level;
    }

    @PostMapping("/upload_avatar")
    public Result<String> uploadAvatar(
            @RequestParam Long adminId,
            @RequestParam MultipartFile file
    ) {
        return adminService.uploadAvatar(adminId, file);
    }

    @PostMapping("/update_info")
    public Result<AdminEntity> updateInfo(@RequestBody InfoAns info) {
        return adminService.updateInfo(info);
    }


    @GetMapping("/get_coaches_by_school")
    public Result<List<CoachEntity>> getCoachesBySchoolId(
            @RequestParam String token,
            @RequestParam Long schoolId) {
        return adminService.getCoachesBySchoolId(token, schoolId);
    }

    @GetMapping("/get_students_by_school")
    public Result<List<StudentEntity>> getStudentsBySchoolId(
            @RequestParam String token,
            @RequestParam Long schoolId) {
        return adminService.getStudentsBySchoolId(token, schoolId);
    }

    // 查看所辖校区学生列表（分页）
    @GetMapping("/students")
    public Result<Page<StudentEntity>> getManagedStudents(
            @RequestParam String token,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,  // 页码，默认第1页
            @RequestParam(defaultValue = "10") Integer pageSize) {  // 每页条数，默认10条
        return adminService.getStudentsBySchoolIdWithPage(token, schoolId, name, pageNum, pageSize);
    }

    // 查看所辖校区已认证教练列表（分页）
    @GetMapping("/certified-coaches")
    public Result<Page<CoachEntity>> getManagedCertifiedCoaches(
            @RequestParam String token,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer level,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return adminService.getCertifiedCoachesBySchoolIdWithPage(token, schoolId, name, level, pageNum, pageSize);
    }

    // 原有修改接口保持不变...
    @PostMapping("/update-student")
    public Result<StudentEntity> updateStudent(
            @RequestParam String token,
            @RequestBody StudentEntity student) {
        return adminService.updateStudent(token, student);
    }

    @PostMapping("/update-certified-coach")
    public Result<CoachEntity> updateCertifiedCoach(
            @RequestParam String token,
            @RequestBody CoachEntity coach) {
        return adminService.updateCertifiedCoach(token, coach);
    }

    /**
     * 管理员给学生线下充值
     */
    @PostMapping("/recharge")
    public Result<PaymentRecordEntity> offlineRecharge(
            @RequestParam Long studentId,
            @RequestParam Double amount) {
        return paymentService.adminOfflineRecharge(studentId, amount);
    }
}
