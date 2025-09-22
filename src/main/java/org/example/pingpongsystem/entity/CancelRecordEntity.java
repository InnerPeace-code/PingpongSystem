package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class CancelRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;  // 学员ID（不能为空）

    @Column(nullable = false)
    private Long coachId;  // 教练ID（不能为空）

    @Column(nullable = false)
    private String userType;  // 用户类型（STUDENT/COACH）- 标识发起人

    @Column(nullable = false)
    private Long appointmentId;  // 关联预约ID

    @Column(nullable = false)
    private LocalDateTime createTime;  // 取消申请时间

    @Enumerated(EnumType.STRING)
    private CancelStatus status;  // 取消状态

    public enum CancelStatus {
        PENDING,  // 待对方确认
        APPROVED, // 已确认
        REJECTED  // 已拒绝
    }
}