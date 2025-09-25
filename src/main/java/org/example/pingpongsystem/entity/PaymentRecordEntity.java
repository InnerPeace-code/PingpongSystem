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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod; // 支付方式

    private String qrCodeUrl; // 二维码图片地址（线上支付用）

    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // 支付状态

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime payTime; // 支付时间

    private LocalDateTime refundTime; // 退款时间

    public enum PaymentMethod {
        WECHAT,   // 微信支付
        ALIPAY,   // 支付宝支付
        OFFLINE,  // 线下充值(管理员代充)
        ACCOUNT   // 账户余额支付
    }

    public enum PaymentStatus {
        PENDING,  // 待支付
        SUCCESS,  // 支付成功
        FAILED,   // 支付失败
        REFUNDED  // 已退款
    }
}

