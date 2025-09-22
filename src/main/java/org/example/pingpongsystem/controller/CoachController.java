package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.dto.CoachStudentDTO;
import org.example.pingpongsystem.entity.CoachEntity;
import org.example.pingpongsystem.entity.CoachTeachStudentEntity;
import org.example.pingpongsystem.entity.StudentEntity;
import org.example.pingpongsystem.service.CoachService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/coach")
@RestController
public class CoachController {
    private final CoachService coachService;
    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @PostMapping("/create_user")
    public Result<String> createUser(CoachEntity coachEntity, @RequestParam MultipartFile file) {
        System.out.println("教练性别" + coachEntity.getIsMale());
        Result<String> result = coachService.save(coachEntity, file);
        return result;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody SuperAdminController.LoginRequest request) {
        return coachService.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/update_info")
    public Result<CoachEntity> updateInfo(@RequestBody CoachEntity coachEntity) {
        System.out.println(coachEntity.getAvatar());
        return coachService.revise(coachEntity);
    }

    @GetMapping("/selected_by_student")
    public Result<List<CoachStudentDTO>> selectedByStudent(@RequestParam Long coachId) {
        return coachService.getStudentSelect(coachId);
    }

    @PostMapping("/review_student_select")
    public Result<CoachTeachStudentEntity> reviewStudentSelect(@RequestParam Long coachTeachStudentId, @RequestParam boolean isAccepted) {
        return coachService.reviewStudentSelect(coachTeachStudentId, isAccepted);
    }

    @PostMapping("/upload_avatar")
    public Result<String> uploadAvatar(
            @RequestParam Long coachId,
            @RequestParam MultipartFile file
    ) {
        return coachService.uploadAvatar(coachId, file);
    }

    @GetMapping("/get_related_students")
    public Result<List<StudentEntity>> getRelatedStudents(@RequestParam Long coachId) {
        return coachService.getRelatedStudents(coachId);
    }

    @GetMapping("/get_student_detail")
    public Result<StudentEntity> getCoachStudentDetail(@RequestParam Long studentId) {
        return coachService.getCoachStudentDetail(studentId);
    }

}
