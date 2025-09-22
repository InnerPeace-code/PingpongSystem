package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class CoachTransactionRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long coachId; // 教练ID

    @Column(nullable = false)
    private Double amount; // 交易金额（正数为收入，负数为支出）

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // 交易类型

    private Long relatedId; // 关联ID（课程预约ID或提现申请ID）

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status; // 交易状态

    private LocalDateTime createTime; // 交易时间

    // 交易类型枚举
    public enum TransactionType {
        COURSE_INCOME, // 课程收入
        WITHDRAW // 提现
    }

    // 交易状态枚举
    public enum TransactionStatus {
        SUCCESS, // 成功
        PENDING, // 处理中
        FAILED // 失败
    }

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
    }
}