package org.example.pingpongsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.NotificationEntity;
import org.example.pingpongsystem.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 创建通用通知
     */
    @Transactional
    public void createNotification(
            Long userId,
            NotificationEntity.UserType userType,
            NotificationEntity.NotificationType type,
            Long appointmentId,
            Long changeRequestId,
            String content) {

        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(userId);
        notification.setUserType(userType);
        notification.setType(type);
        notification.setAppointmentId(appointmentId);
        notification.setChangeRequestId(changeRequestId);
        notification.setContent(content);
        notification.setRead(false);
        notification.setCreateTime(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    /**
     * 创建课程评价通知（保持原有功能）
     */
    @Transactional
    public void createEvaluationNotification(Long userId, NotificationEntity.UserType userType, Long appointmentId) {
        String content = "您有一节课程已完成，请进行评价";
        createNotification(userId, userType, NotificationEntity.NotificationType.COURSE_EVALUATION,
                appointmentId, null, content);
    }

    /**
     * 创建更换教练申请通知
     */
    @Transactional
    public void createCoachChangeRequestNotification(
            Long userId,
            NotificationEntity.UserType userType,
            Long changeRequestId,
            String content) {

        createNotification(userId, userType, NotificationEntity.NotificationType.COACH_CHANGE_REQUEST,
                null, changeRequestId, content);
    }

    /**
     * 创建课程提醒通知
     */
    @Transactional
    public void createCourseReminderNotification(Long userId, NotificationEntity.UserType userType, Long appointmentId) {
        String content = "您有一节课程将在1小时后开始，请做好准备";
        createNotification(userId, userType, NotificationEntity.NotificationType.COURSE_REMINDER,
                appointmentId, null, content);
    }

    /**
     * 获取用户未读通知
     */
    public List<NotificationEntity> getUnreadNotifications(Long userId, NotificationEntity.UserType userType) {
        return notificationRepository.findByUserIdAndUserTypeAndIsReadFalse(userId, userType);
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public void markNotificationAsRead(Long notificationId) {
        notificationRepository.findById(notificationId)
                .ifPresent(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                });
    }
}