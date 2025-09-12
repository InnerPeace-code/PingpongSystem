package org.example.pingpongsystem.controller;

import lombok.Data;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.service.AdminService;
import org.example.pingpongsystem.service.CoachService;
import org.example.pingpongsystem.service.SuperAdminService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/admin")
@RestController
public class AdminController {
    private final AdminService adminService;
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
}
