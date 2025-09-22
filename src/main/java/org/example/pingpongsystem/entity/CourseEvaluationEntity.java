package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class CourseEvaluationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long appointmentId;  // 关联的课程预约ID

    @Column(nullable = false)
    private Long evaluatorId;    // 评价人ID（学员或教练）

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluatorType evaluatorType;  // 评价人类型

    @Column(nullable = false)
    private String content;  // 评价内容

    @Column(nullable = false)
    private LocalDateTime createTime;  // 评价创建时间

    @Column(nullable = false)
    private LocalDateTime updateTime;  // 评价更新时间（新增）

    // 评价人类型枚举
    public enum EvaluatorType {
        STUDENT,  // 学员
        COACH     // 教练
    }

    // 新增：创建时自动设置创建/更新时间
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    // 新增：更新时自动更新updateTime
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}