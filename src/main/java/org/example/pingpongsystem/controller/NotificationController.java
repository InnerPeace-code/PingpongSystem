package org.example.pingpongsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.NotificationEntity;
import org.example.pingpongsystem.service.NotificationService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/notifications")
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 查询用户的未读通知（供前端轮询或实时查询）
     */
    @GetMapping("/unread")
    public Result<List<NotificationEntity>> getUnreadNotifications(
            @RequestParam Long userId,
            @RequestParam NotificationEntity.UserType userType) {

        List<NotificationEntity> unread = notificationService.getUnreadNotifications(userId, userType);
        return Result.success(unread);
    }

    /**
     * 标记通知为已读（用户查看后调用）
     */
    @PostMapping("/mark-read")
    public Result<Void> markAsRead(@RequestParam Long notificationId) {
        notificationService.markNotificationAsRead(notificationId);
        return Result.success();
    }
}