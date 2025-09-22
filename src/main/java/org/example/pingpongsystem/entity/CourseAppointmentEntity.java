package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class CourseAppointmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long coachId;  // 关联教练ID

    @Column(nullable = false)
    private Long studentId;  // 关联学员ID

    @Column(nullable = false)
    private Long schoolId;  // 校区ID，用于快速查询

    @Column(nullable = false)
    private LocalDateTime startTime;  // 课程开始时间

    @Column(nullable = false)
    private LocalDateTime endTime;  // 课程结束时间

    @Column(nullable = false)
    private Long tableId;  // 球台ID

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;  // 预约状态

    @Column(nullable = false)
    private Double amount;  // 课时费用

    private Long paymentRecordId;  // 关联支付记录ID

    @Version
    private Integer version;

    // 预约状态枚举
    public enum AppointmentStatus {
        PENDING_CONFIRM,  // 待教练确认
        CONFIRMED,        // 已确认
        CANCEL_REQUESTED, // 已发起取消申请
        CANCELLED,        // 已取消
        REJECTED,          // 已拒绝
        COMPLETED         // 已完成（课程结束）
    }
}