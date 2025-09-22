package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class CoachChangeRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId; // 学员ID

    @Column(nullable = false)
    private Long currentCoachId; // 当前教练ID

    @Column(nullable = false)
    private Long targetCoachId; // 目标教练ID

    @Column(nullable = false)
    private Long schoolId; // 校区ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status; // 申请状态

    private Boolean currentCoachApproval; // 当前教练是否同意
    private Boolean targetCoachApproval; // 目标教练是否同意
    private Boolean adminApproval; // 管理员是否同意

    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间

    // 申请状态枚举
    public enum Status {
        PENDING, // 待审批
        APPROVED, // 全部同意
        REJECTED // 有人拒绝
    }
}