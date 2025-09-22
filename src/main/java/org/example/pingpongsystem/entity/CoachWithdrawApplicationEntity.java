package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class CoachWithdrawApplicationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long coachId; // 教练ID

    @Column(nullable = false)
    private Double amount; // 提现金额

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawStatus status; // 提现状态

    private String bankAccount; // 银行账户

    private String bankName; // 银行名称

    private String accountHolder; // 账户持有人

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime completeTime; // 完成时间

    // 提现状态枚举
    public enum WithdrawStatus {
        PENDING, // 待处理
        COMPLETED, // 已完成
        REJECTED // 已拒绝
    }

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        status = WithdrawStatus.PENDING;
    }
}
