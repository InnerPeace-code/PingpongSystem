package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class StudentAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId; // 关联学员ID

    private Double balance = 0.0; // 账户余额，默认0

    @Version
    private Integer version; // 乐观锁
}