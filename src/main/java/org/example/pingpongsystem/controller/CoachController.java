package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.entity.CoachEntity;
import org.example.pingpongsystem.entity.CoachTeachStudentEntity;
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
        Result<String> result = coachService.save(coachEntity, file);
        return result;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody SuperAdminController.LoginRequest request) {
        return coachService.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/update_info")
    public Result<CoachEntity> updateInfo(@RequestBody CoachEntity coachEntity, MultipartFile file) {
        return coachService.revise(coachEntity, file);
    }

    @GetMapping("/selected_by_student")
    public Result<List<CoachTeachStudentEntity>> selectedByStudent(@RequestParam Long coachId) {
        return coachService.getStudentSelect(coachId);
    }

    @PostMapping("/review_student_select")
    public Result<CoachTeachStudentEntity> reviewStudentSelect(@RequestParam Long coachTeachStudentId, @RequestParam boolean isAccepted) {
        return coachService.reviewStudentSelect(coachTeachStudentId, isAccepted);
    }
}
