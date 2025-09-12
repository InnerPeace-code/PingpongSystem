package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.entity.CoachEntity;
import org.example.pingpongsystem.entity.CoachTeachStudentEntity;
import org.example.pingpongsystem.entity.StudentEntity;
import org.example.pingpongsystem.service.CoachService;
import org.example.pingpongsystem.service.StudentService;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.web.bind.annotation.*;

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
        boolean result = studentService.save(studentEntity);
        if (result) {
            return Result.success("保存成功"); // 返回 { "code":20000, "data":"保存成功" }
        } else {
            return Result.error(StatusCode.FAIL,"用户名、密码、校区、姓名和电话是必须填写的信息，其他信息可空白");
        }
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
    public Result<List<CoachEntity>> getCoachList(@RequestParam(value = "name") String name,
                                                  @RequestParam(value = "isMale") Boolean isMale,
                                                  @RequestParam(value = "age_low") Integer age_low,
                                                  @RequestParam(value = "age_high") Integer age_high,
                                                  @RequestParam(value = "level") Integer level) {
        return coachService.getSearched(name, isMale, age_low, age_high, level);
    }

    @PostMapping("/select_coach")
    public Result<CoachTeachStudentEntity> selectCoach(@RequestParam(value = "coachId") Long coachId,
                                                       @RequestParam(value = "studentId") Long studentId) {
        return studentService.selectCoach(coachId, studentId);
    }
}
