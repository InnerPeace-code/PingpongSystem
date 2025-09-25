package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class SystemActivation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 唯一激活秘钥
    @Column(unique = true, nullable = false)
    private String secretKey;

    // 绑定的设备标识（如MAC地址、设备UUID等）
    @Column(nullable = false)
    private String deviceId;

    // 激活生效时间
    @Column(nullable = false)
    private LocalDateTime validFrom;

    // 激活过期时间（生效时间+1年）
    @Column(nullable = false)
    private LocalDateTime validTo;

    // 是否激活状态
    @Column(nullable = false)
    private boolean isActive;

    // 关联的超级管理员ID（系统唯一超管）
    @Column(nullable = false)
    private Long superAdminId;
}