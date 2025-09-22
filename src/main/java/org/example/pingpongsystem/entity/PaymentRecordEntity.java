package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class PaymentRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId; // 学员ID

    @Column(nullable = false)
    private Double amount; // 支付金额

    @Column(nullable = false)//前两种方式是学员自行充值，offline是学员找管理员代充，account是学员预约课程扣费
    private String paymentMethod; // 支付方式：WECHAT/ALIPAY/OFFLINE/ACCOUNT

    private String qrCodeUrl; // 二维码图片地址（线上支付用）

    private String status; // 状态：PENDING/SUCCESS/FAILED/REFUNDED

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime payTime; // 支付时间

    private LocalDateTime refundTime; // 退款时间（新增字段）
}