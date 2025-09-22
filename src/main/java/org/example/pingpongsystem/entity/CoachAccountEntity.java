package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CoachAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long coachId; // 教练ID

    @Column(nullable = false)
    private Double balance = 0.0; // 账户余额，默认为0

    @Version
    private Integer version; // 用于乐观锁

}