package org.example.pingpongsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.CoachChangeRequestEntity;
import org.example.pingpongsystem.entity.CoachEntity;
import org.example.pingpongsystem.entity.NotificationEntity;
import org.example.pingpongsystem.service.CoachChangeService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/coach-change")
@RestController
@RequiredArgsConstructor
public class CoachChangeController {

    private final CoachChangeService coachChangeService;

    /**
     * 获取学员当前的教练
     */
    @GetMapping("/current-coaches")
    public Result<List<CoachEntity>> getCurrentCoaches(@RequestParam Long studentId) {
        return coachChangeService.getCurrentCoaches(studentId);
    }

    /**
     * 获取学员所在校区的所有教练
     */
    @GetMapping("/school-coaches")
    public Result<List<CoachEntity>> getSchoolCoaches(@RequestParam Long studentId) {
        return coachChangeService.getSchoolCoaches(studentId);
    }

    /**
     * 提交更换教练申请
     */
    @PostMapping("/submit-request")
    public Result<CoachChangeRequestEntity> submitChangeRequest(
            @RequestParam Long studentId,
            @RequestParam Long currentCoachId,
            @RequestParam Long targetCoachId) {
        return coachChangeService.submitChangeRequest(studentId, currentCoachId, targetCoachId);
    }

    /**
     * 处理更换教练申请
     */
    @PostMapping("/handle-request")
    public Result<CoachChangeRequestEntity> handleChangeRequest(
            @RequestParam Long requestId,
            @RequestParam Long handlerId,
            @RequestParam NotificationEntity.UserType handlerType,
            @RequestParam boolean approve) {
        return coachChangeService.handleChangeRequest(requestId, handlerId, handlerType, approve);
    }

    /**
     * 获取用户相关的更换教练申请
     */
    @GetMapping("/related-requests")
    public Result<List<CoachChangeRequestEntity>> getRelatedRequests(
            @RequestParam Long userId,
            @RequestParam NotificationEntity.UserType userType) {
        return coachChangeService.getRelatedRequests(userId, userType);
    }
}