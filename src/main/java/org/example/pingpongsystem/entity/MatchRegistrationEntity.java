package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class MatchRegistrationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long monthlyMatchId; // 关联月赛ID

    @Column(nullable = false)
    private Long studentId; // 学员ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupType groupType; // 组别

    @Column(nullable = false)
    private boolean paid; // 是否支付

    private Long paymentRecordId; // 支付记录ID

    private LocalDateTime registrationTime; // 报名时间

    @Version
    private Integer version;

    public enum GroupType {
        GROUP_A, GROUP_B, GROUP_C // 甲、乙、丙组
    }
}