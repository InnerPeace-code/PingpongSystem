package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // 接收通知的用户ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType; // 用户类型：STUDENT/COACH/ADMIN

    private Long appointmentId; // 关联的课程预约ID（可为空）
    private Long changeRequestId; // 关联的更换教练申请ID（可为空）

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type; // 通知类型

    @Column(nullable = false)
    private String content; // 通知内容

    private boolean isRead = false; // 是否已读
    private LocalDateTime createTime; // 通知创建时间

    // 用户类型枚举（新增ADMIN）
    public enum UserType {
        STUDENT, COACH, ADMIN
    }

    // 通知类型枚举
    public enum NotificationType {
        COURSE_EVALUATION, // 课程评价通知
        COACH_CHANGE_REQUEST, // 更换教练申请通知
        COACH_CHANGE_APPROVAL, // 更换教练审批结果通知
        COURSE_REMINDER // 课程提醒通知
    }
}