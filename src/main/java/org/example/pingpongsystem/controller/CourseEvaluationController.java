package org.example.pingpongsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.CourseEvaluationEntity;
import org.example.pingpongsystem.service.CourseEvaluationService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/evaluation")
@RestController
@RequiredArgsConstructor
public class CourseEvaluationController {

    private final CourseEvaluationService evaluationService;

    /**
     * 提交评价（新增）
     */
    @PostMapping("/submit")
    public Result<CourseEvaluationEntity> createEvaluation(
            @RequestParam Long appointmentId,
            @RequestParam Long evaluatorId,
            @RequestParam CourseEvaluationEntity.EvaluatorType evaluatorType,
            @RequestParam String content) {
        return evaluationService.createEvaluation(appointmentId, evaluatorId, evaluatorType, content);
    }

    /**
     * 获取课程的所有评价
     */
    @GetMapping("/by_appointment")
    public Result<List<CourseEvaluationEntity>> getEvaluationsByAppointment(
            @RequestParam Long appointmentId) {
        return evaluationService.getEvaluationsByAppointment(appointmentId);
    }

    /**
     * 获取用户的评价记录
     */
    @GetMapping("/by_user")
    public Result<List<CourseEvaluationEntity>> getEvaluationsByUser(
            @RequestParam Long userId,
            @RequestParam CourseEvaluationEntity.EvaluatorType type) {
        return evaluationService.getEvaluationsByUser(userId, type);
    }

    /**
     * 编辑评价（新增，PUT请求保证幂等性）
     */
    @PutMapping("/update")
    public Result<CourseEvaluationEntity> updateEvaluation(
            @RequestParam Long evaluationId,    // 要编辑的评价ID
            @RequestParam String content,        // 新评价内容
            @RequestParam Long evaluatorId) {    // 评价人ID（权限校验）
        return evaluationService.updateEvaluation(evaluationId, content, evaluatorId);
    }

    /**
     * 删除评价（新增）
     */
    @DeleteMapping("/delete")
    public Result<Boolean> deleteEvaluation(
            @RequestParam Long evaluationId,    // 要删除的评价ID
            @RequestParam Long evaluatorId) {    // 评价人ID（权限校验）
        return evaluationService.deleteEvaluation(evaluationId, evaluatorId);
    }
}