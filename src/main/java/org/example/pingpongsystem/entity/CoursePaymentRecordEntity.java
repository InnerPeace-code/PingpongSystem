package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class CoursePaymentRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long appointmentId; // 预约ID，唯一

    @Column(nullable = false)
    private Long coachId; // 教练ID

    @Column(nullable = false)
    private Double amount; // 付款金额

    @Column(nullable = false)
    private Long paymentRecordId; // 关联的支付记录ID

    private LocalDateTime paymentTime; // 付款时间

    @PrePersist
    public void prePersist() {
        paymentTime = LocalDateTime.now();
    }
}
