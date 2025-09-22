package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;

@Entity
@Data
public class ScheduleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long schoolId; // 关联校区ID

    @Column(nullable = false)
    private Integer dayOfWeek; // 星期几（1=周一，2=周二，...，6=周六，7=周日）

    @Column(nullable = false)
    private LocalTime startTime; // 开始时间

    @Column(nullable = false)
    private LocalTime endTime; // 结束时间

    private String description; // 课程描述

    @Version
    private Integer version;
}