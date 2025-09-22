package org.example.pingpongsystem.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.CoachAccountEntity;
import org.example.pingpongsystem.entity.CoachTransactionRecordEntity;
import org.example.pingpongsystem.entity.CoachWithdrawApplicationEntity;
import org.example.pingpongsystem.repository.CoachAccountRepository;
import org.example.pingpongsystem.repository.CoachTransactionRecordRepository;
import org.example.pingpongsystem.repository.CoachWithdrawApplicationRepository;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CoachAccountService {
    private final CoachAccountRepository coachAccountRepository;
    private final CoachTransactionRecordRepository transactionRecordRepository;
    private final CoachWithdrawApplicationRepository withdrawApplicationRepository;

    /**
     * 为教练创建账户
     */
    @Transactional
    public void createCoachAccount(Long coachId) {
        // 检查是否已存在账户
        if (!coachAccountRepository.existsByCoachId(coachId)) {
            CoachAccountEntity account = new CoachAccountEntity();
            account.setCoachId(coachId);
            account.setBalance(0.0);
            coachAccountRepository.save(account);
        }
    }

    /**
     * 给教练账户增加金额（课程收入）
     */
    @Transactional
    public Result<String> addBalance(Long coachId, Double amount, Long appointmentId) {
        if (amount <= 0) {
            return Result.error(StatusCode.FAIL, "金额必须大于0");
        }

        CoachAccountEntity account = coachAccountRepository.findByCoachId(coachId)
                .orElseThrow(() -> new RuntimeException("教练账户不存在"));

        // 更新余额
        account.setBalance(account.getBalance() + amount);
        coachAccountRepository.save(account);

        // 记录交易
        CoachTransactionRecordEntity transaction = new CoachTransactionRecordEntity();
        transaction.setCoachId(coachId);
        transaction.setAmount(amount);
        transaction.setType(CoachTransactionRecordEntity.TransactionType.COURSE_INCOME);
        transaction.setRelatedId(appointmentId);
        transaction.setStatus(CoachTransactionRecordEntity.TransactionStatus.SUCCESS);
        transactionRecordRepository.save(transaction);

        return Result.success("账户已更新");
    }

    /**
     * 获取教练账户余额
     */
    public Result<Double> getBalance(Long coachId) {
        CoachAccountEntity account = coachAccountRepository.findByCoachId(coachId)
                .orElseThrow(() -> new RuntimeException("教练账户不存在"));

        return Result.success(account.getBalance());
    }

    /**
     * 获取教练交易记录
     */
    public Result<Page<CoachTransactionRecordEntity>> getTransactions(
            Long coachId, String type, Pageable pageable) {
        Page<CoachTransactionRecordEntity> transactions;

        if (type != null && !type.isEmpty()) {
            try {
                CoachTransactionRecordEntity.TransactionType transactionType =
                        CoachTransactionRecordEntity.TransactionType.valueOf(type);
                transactions = transactionRecordRepository
                        .findByCoachIdAndTypeOrderByCreateTimeDesc(coachId, transactionType, pageable);
            } catch (IllegalArgumentException e) {
                return Result.error(StatusCode.FAIL, "无效的交易类型");
            }
        } else {
            transactions = transactionRecordRepository
                    .findByCoachIdOrderByCreateTimeDesc(coachId, pageable);
        }

        return Result.success(transactions);
    }

    /**
     * 提交提现申请（模拟实现）
     */
    @Transactional
    public Result<CoachWithdrawApplicationEntity> applyWithdraw(
            Long coachId, Double amount, String bankAccount, String bankName, String accountHolder) {
        // 验证金额
        if (amount <= 0) {
            return Result.error(StatusCode.FAIL, "提现金额必须大于0");
        }

        // 验证账户信息
        if (bankAccount == null || bankAccount.isEmpty() ||
                bankName == null || bankName.isEmpty() ||
                accountHolder == null || accountHolder.isEmpty()) {
            return Result.error(StatusCode.FAIL, "银行账户信息不完整");
        }

        // 检查余额
        CoachAccountEntity account = coachAccountRepository.findByCoachId(coachId)
                .orElseThrow(() -> new RuntimeException("教练账户不存在"));

        if (account.getBalance() < amount) {
            return Result.error(StatusCode.FAIL, "账户余额不足");
        }

        // 创建提现申请
        CoachWithdrawApplicationEntity application = new CoachWithdrawApplicationEntity();
        application.setCoachId(coachId);
        application.setAmount(amount);
        application.setBankAccount(bankAccount);
        application.setBankName(bankName);
        application.setAccountHolder(accountHolder);

        CoachWithdrawApplicationEntity savedApplication = withdrawApplicationRepository.save(application);

        // 模拟即时处理提现（无需审核，直接完成）
        return processWithdraw(savedApplication.getId());
    }

    /**
     * 处理提现申请（模拟自动通过）
     */
    @Transactional
    public Result<CoachWithdrawApplicationEntity> processWithdraw(Long applicationId) {
        CoachWithdrawApplicationEntity application = withdrawApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("提现申请不存在"));

        // 如果已经处理过，直接返回
        if (application.getStatus() != CoachWithdrawApplicationEntity.WithdrawStatus.PENDING) {
            return Result.success(application);
        }

        // 模拟处理：直接通过
        application.setStatus(CoachWithdrawApplicationEntity.WithdrawStatus.COMPLETED);
        application.setCompleteTime(LocalDateTime.now());
        CoachWithdrawApplicationEntity updatedApplication = withdrawApplicationRepository.save(application);

        // 扣减账户余额
        CoachAccountEntity account = coachAccountRepository.findByCoachId(application.getCoachId())
                .orElseThrow(() -> new RuntimeException("教练账户不存在"));

        account.setBalance(account.getBalance() - application.getAmount());
        coachAccountRepository.save(account);

        // 记录交易
        CoachTransactionRecordEntity transaction = new CoachTransactionRecordEntity();
        transaction.setCoachId(application.getCoachId());
        transaction.setAmount(-application.getAmount()); // 负数表示支出
        transaction.setType(CoachTransactionRecordEntity.TransactionType.WITHDRAW);
        transaction.setRelatedId(application.getId());
        transaction.setStatus(CoachTransactionRecordEntity.TransactionStatus.SUCCESS);
        transactionRecordRepository.save(transaction);

        return Result.success(updatedApplication);
    }
}
