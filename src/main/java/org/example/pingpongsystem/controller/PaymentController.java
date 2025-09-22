// PingpongSystem/src/main/java/org/example/pingpongsystem/controller/PaymentController.java
package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.entity.PaymentRecordEntity;
import org.example.pingpongsystem.service.PaymentService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/payment")
@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // 获取余额
    @GetMapping("/balance")
    public Result<Double> getBalance(@RequestParam Long studentId) {
        return paymentService.getBalance(studentId);
    }

    // 创建支付订单
    @PostMapping("/create")
    public Result<PaymentRecordEntity> createPayment(
            @RequestParam Long studentId,
            @RequestParam Double amount,
            @RequestParam String method) {
        return paymentService.createPayment(studentId, amount, method);
    }

    // 确认支付
    @PostMapping("/confirm")
    public Result<String> confirmPayment(@RequestParam Long recordId) {
        return paymentService.confirmPayment(recordId);
    }

    // 取消支付
    @PostMapping("/cancel")
    public Result<String> cancelPayment(@RequestParam Long recordId) {
        return paymentService.cancelPayment(recordId);
    }

    // 新增：获取交易记录
    @GetMapping("/records")
    public Result<Page<PaymentRecordEntity>> getPaymentRecords(
            @RequestParam Long studentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method) {
        // 前端页码从1开始，转换为后端从0开始的页码，同时确保页码不小于0
        int actualPage = Math.max(page - 1, 0);
        return paymentService.getPaymentRecords(studentId, actualPage, size, status, method);
    }
}