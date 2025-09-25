package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class MonthlyMatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // 比赛标题

    @Column(nullable = false)
    private LocalDateTime startTime; // 比赛开始时间

    @Column(nullable = false)
    private LocalDateTime registrationDeadline; // 报名截止时间

    @Column(nullable = false)
    private Integer year; // 年份

    @Column(nullable = false)
    private Integer month; // 月份

    @Transient  // 关键：标注该字段不存入数据库
    private Boolean hasSchedule;

    @Enumerated(EnumType.STRING)
    private MatchStatus status; // 比赛状态

    public enum MatchStatus {
        NOT_STARTED,// 初始状态：未到报名时间
        REGISTERING,// 报名中：已到报名时间 + 未过截止时间
        REGISTRATION_CLOSED,// 报名截止：已过截止时间（未安排赛程）
        ONGOING,// 比赛进行中（可选，非必需）
        COMPLETED // 比赛结束（可选，非必需）
    }
}