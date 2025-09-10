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

@RequestMapping("/super_admin")
@RestController
public class SuperAdminController {
    private final SuperAdminService superAdminService;
    private final AdminService adminService;

    public SuperAdminController(SuperAdminService superAdminService, AdminService adminService) {
        this.superAdminService = superAdminService;
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest request) {
        return superAdminService.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/create_admin")
    public Result<AdminEntity> createUser(@RequestBody AdminEntity adminEntity) {
        return superAdminService.createAdmin(adminEntity);
    }

    @GetMapping("/schools")
    public Result<List<SchoolEntity>> getAllSchools() {
        return superAdminService.getAllSchools();
    }

    @PostMapping("/create_school")
    public Result<SchoolEntity> createSchool(@RequestBody SchoolRequest request) {
        SchoolEntity schoolEntity = new SchoolEntity();
        schoolEntity.setSchoolname(request.getSchoolname()); // 新增：设置校区名称
        schoolEntity.setName(request.getName());
        schoolEntity.setAddress(request.getLocation());
        schoolEntity.setTable_num(request.getTableCount());
        schoolEntity.setAdminId(request.getAdminId());
        // 补充设置其他字段（phone、email等，前端已传递）
        schoolEntity.setPhone(request.getPhone()); // 如果SchoolRequest中也添加了phone
        schoolEntity.setEmail(request.getEmail()); // 如果SchoolRequest中也添加了email
        return superAdminService.createSchool(schoolEntity);
    }

    @PostMapping("/manage_school_info")
    public Result<SchoolEntity> manageSchoolInfo(@RequestBody SchoolEntity schoolEntity) {
        return superAdminService.reviseSchool(schoolEntity);
    }

    @DeleteMapping("/delete_school/{id}")
    public Result<Void> deleteSchool(@PathVariable Long id) {
        return superAdminService.deleteSchool(id);
    }
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        private String role; // 新增角色字段
    }

    @Data
    public static class SchoolRequest {
        private String name;
        private String location;
        private int tableCount;
        private int adminId;
        private String schoolname; // 校区名称
        private String phone; // 联系电话
        private String email; // 联系邮箱
    }
}
