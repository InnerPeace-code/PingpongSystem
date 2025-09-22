package org.example.pingpongsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.CoachTransactionRecordEntity;
import org.example.pingpongsystem.entity.CoachWithdrawApplicationEntity;
import org.example.pingpongsystem.service.CoachAccountService;
import org.example.pingpongsystem.utility.Result;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/coach/account")
@RestController
@RequiredArgsConstructor
public class CoachAccountController {
    private final CoachAccountService coachAccountService;

    /**
     * 查询教练账户余额
     */
    @GetMapping("/balance")
    public Result<Double> getBalance(@RequestParam Long coachId) {
        return coachAccountService.getBalance(coachId);
    }

    /**
     * 获取教练交易记录
     */
    @GetMapping("/transactions")
    public Result<Page<CoachTransactionRecordEntity>> getTransactions(
            @RequestParam Long coachId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Spring Data JPA的页码是从0开始的，所以需要减1
        Pageable pageable = PageRequest.of(page - 1, size);
        return coachAccountService.getTransactions(coachId, type, pageable);
    }

    /**
     * 提交提现申请
     */
    @PostMapping("/withdraw")
    public Result<CoachWithdrawApplicationEntity> applyWithdraw(
            @RequestParam Long coachId,
            @RequestParam Double amount,
            @RequestParam String bankAccount,
            @RequestParam String bankName,
            @RequestParam String accountHolder) {
        return coachAccountService.applyWithdraw(coachId, amount, bankAccount, bankName, accountHolder);
    }
}