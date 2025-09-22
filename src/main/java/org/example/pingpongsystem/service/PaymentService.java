package org.example.pingpongsystem.service;

import org.example.pingpongsystem.entity.PaymentRecordEntity;
import org.example.pingpongsystem.entity.StudentAccountEntity;
import org.example.pingpongsystem.repository.PaymentRecordRepository;
import org.example.pingpongsystem.repository.StudentAccountRepository;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;

@Service
public class PaymentService {
    private final StudentAccountRepository accountRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    public PaymentService(StudentAccountRepository accountRepository, PaymentRecordRepository paymentRecordRepository) {
        this.accountRepository = accountRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    // 获取学员账户余额
    public Result<Double> getBalance(Long studentId) {
        StudentAccountEntity account = accountRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    // 首次查询创建账户
                    StudentAccountEntity newAccount = new StudentAccountEntity();
                    newAccount.setStudentId(studentId);
                    return accountRepository.save(newAccount);
                });
        return Result.success(account.getBalance());
    }

    // 创建支付订单（生成二维码）- 正数表示充值
    public Result<PaymentRecordEntity> createPayment(Long studentId, Double amount, String method) {
        if (amount <= 0) {
            return Result.error(StatusCode.FAIL, "金额必须大于0");
        }
        if (!"WECHAT".equals(method) && !"ALIPAY".equals(method) && !"OFFLINE".equals(method)) {
            return Result.error(StatusCode.FAIL, "不支持的支付方式");
        }

        PaymentRecordEntity record = new PaymentRecordEntity();
        record.setStudentId(studentId);
        record.setAmount(amount); // 充值金额为正数
        record.setPaymentMethod(method);
        record.setStatus("PENDING");
        record.setCreateTime(LocalDateTime.now());

        // 只有线上支付才生成二维码
        if ("WECHAT".equals(method) || "ALIPAY".equals(method)) {
            record.setQrCodeUrl("/qrcode/" + UUID.randomUUID() + ".png");
        }

        paymentRecordRepository.save(record);
        return Result.success(record);
    }

    // 模拟支付结果回调（扫码后确认支付）
    @Transactional
    public Result<String> confirmPayment(Long recordId) {
        PaymentRecordEntity record = paymentRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"PENDING".equals(record.getStatus())) {
            return Result.error(StatusCode.FAIL, "订单状态异常");
        }

        // 更新订单状态
        record.setStatus("SUCCESS");
        record.setPayTime(LocalDateTime.now());
        paymentRecordRepository.save(record);

        // 更新账户余额（充值金额为正数，添加到余额）
        StudentAccountEntity account = accountRepository.findByStudentId(record.getStudentId())
                .orElseGet(() -> {
                    StudentAccountEntity newAccount = new StudentAccountEntity();
                    newAccount.setStudentId(record.getStudentId());
                    return newAccount;
                });
        account.setBalance(account.getBalance() + record.getAmount());
        accountRepository.save(account);

        return Result.success("支付成功，余额已更新");
    }

    // 取消支付
    public Result<String> cancelPayment(Long recordId) {
        PaymentRecordEntity record = paymentRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"PENDING".equals(record.getStatus())) {
            return Result.error(StatusCode.FAIL, "订单状态异常");
        }

        record.setStatus("FAILED");
        paymentRecordRepository.save(record);
        return Result.success("已取消支付");
    }

    // 退款方法 - 正数表示退款（资金回流）
    @Transactional
    public Result<String> refund(Long recordId) {
        PaymentRecordEntity originalRecord = paymentRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("支付记录不存在"));

        if (!"SUCCESS".equals(originalRecord.getStatus())) {
            return Result.error(StatusCode.FAIL, "只有已支付的订单可以退款");
        }

        // 创建新的退款记录（正数）
        PaymentRecordEntity refundRecord = new PaymentRecordEntity();
        refundRecord.setStudentId(originalRecord.getStudentId());
        refundRecord.setAmount(-originalRecord.getAmount()); // 退款金额为正数,负负得正
        refundRecord.setPaymentMethod(originalRecord.getPaymentMethod());
        refundRecord.setStatus("REFUNDED");
        refundRecord.setCreateTime(LocalDateTime.now());
        refundRecord.setPayTime(LocalDateTime.now());
        refundRecord.setRefundTime(LocalDateTime.now());
        paymentRecordRepository.save(refundRecord);

        // 更新原始记录状态
        //originalRecord.setStatus("REFUNDED");
        paymentRecordRepository.save(originalRecord);

        // 退还金额到学员账户
        StudentAccountEntity account = accountRepository.findByStudentId(originalRecord.getStudentId())
                .orElseThrow(() -> new RuntimeException("学员账户不存在"));
        account.setBalance(account.getBalance() + originalRecord.getAmount()); // 加上退款金额
        accountRepository.save(account);

        return Result.success("退款成功");
    }

    // 新增：创建课程消费记录（负数表示支出）
    @Transactional
    public PaymentRecordEntity createCoursePaymentRecord(Long studentId, Double amount) {
        PaymentRecordEntity record = new PaymentRecordEntity();
        record.setStudentId(studentId);
        record.setAmount(-amount); // 消费金额为负数
        record.setPaymentMethod("ACCOUNT");
        record.setStatus("SUCCESS");
        record.setCreateTime(LocalDateTime.now());
        record.setPayTime(LocalDateTime.now());

        return paymentRecordRepository.save(record);
    }

    // 新增：获取交易记录
    public Result<Page<PaymentRecordEntity>> getPaymentRecords(
            Long studentId, int page, int size, String status, String method) {

        // 创建分页和排序条件（按创建时间倒序）
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        // 动态构建查询条件
        Specification<PaymentRecordEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 必须满足的条件：学生ID
            predicates.add(cb.equal(root.get("studentId"), studentId));

            // 可选条件：状态筛选
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 可选条件：支付方式筛选
            if (method != null && !method.isEmpty()) {
                predicates.add(cb.equal(root.get("paymentMethod"), method));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PaymentRecordEntity> records = paymentRecordRepository.findAll(spec, pageable);
        return Result.success(records);
    }
}