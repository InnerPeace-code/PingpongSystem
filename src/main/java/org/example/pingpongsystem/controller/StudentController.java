package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.entity.CoachEntity;
import org.example.pingpongsystem.entity.CoachTeachStudentEntity;
import org.example.pingpongsystem.entity.StudentEntity;
import org.example.pingpongsystem.service.CoachService;
import org.example.pingpongsystem.service.StudentService;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/student")
@RestController
public class StudentController {
    private final StudentService studentService;
    private final CoachService coachService;

    public StudentController(StudentService studentService, CoachService coachService) {
        this.studentService = studentService;
        this.coachService = coachService;
    }

    @PostMapping("/create_user")
    public Result<String> createUser(@RequestBody StudentEntity studentEntity) {
        System.out.println("学生性别 " + studentEntity.isMale());
        Result<String> result = studentService.save(studentEntity);
        return result;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody SuperAdminController.LoginRequest request) {
        return studentService.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/update_info")
    public Result<StudentEntity> updateInfo(@RequestBody StudentEntity studentEntity) {
        return studentService.revise(studentEntity);
    }

    @GetMapping("/get_coach_list")
    public Result<List<CoachEntity>> getCoachList(
            @RequestParam(value = "name", required = false) String name,  // 允许为null
            @RequestParam(value = "isMale", required = false) Boolean isMale,
            @RequestParam(value = "age_low", required = false) Integer age_low,
            @RequestParam(value = "age_high", required = false) Integer age_high,
            @RequestParam(value = "level", required = false) Integer level,
            @RequestParam(value = "schoolId", required = false) Long schoolId) {  // 新增校区ID参数
        return coachService.getSearched(name, isMale, age_low, age_high, level, schoolId);
    }

    @PostMapping("/select_coach")
    public Result<CoachTeachStudentEntity> selectCoach(@RequestParam(value = "coachId") Long coachId,
                                                       @RequestParam(value = "studentId") Long studentId) {
        return studentService.selectCoach(coachId, studentId);
    }

    @PostMapping("/upload_avatar")
    public Result<String> uploadAvatar(
            @RequestParam Long studentId,
            @RequestParam MultipartFile file
    ) {
        return studentService.uploadAvatar(studentId, file);
    }

    @GetMapping("/get_coach_detail")
    public Result<CoachEntity> getStudentCoachDetail(@RequestParam Long coachId) {
        return studentService.getStudentCoachDetail(coachId);
    }

    @GetMapping("/get_related_coaches")
    public Result<List<CoachEntity>> getRelatedCoaches(@RequestParam Long studentId) {
        return studentService.getRelatedCoaches(studentId);
    }

    @GetMapping("/get-by-id")
    public Result<StudentEntity> getStudentById(@RequestParam Long studentId) {
        return studentService.getStudentById(studentId);
    }
}
