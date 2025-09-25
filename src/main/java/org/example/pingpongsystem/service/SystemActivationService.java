package org.example.pingpongsystem.service;

import org.example.pingpongsystem.dto.ActivationExpiryDTO;
import org.example.pingpongsystem.entity.SystemActivation;
import org.example.pingpongsystem.repository.SystemActivationRepository;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class SystemActivationService {
    private final SystemActivationRepository activationRepository;
    private final PaymentService paymentService;

    public SystemActivationService(SystemActivationRepository activationRepository, PaymentService paymentService) {
        this.activationRepository = activationRepository;
        this.paymentService = paymentService;
    }

    // 检查系统是否已激活（所有用户访问前需验证）
    public boolean isSystemActivated() {
        return activationRepository.findTopByIsActiveTrue()
                .map(activation -> LocalDateTime.now().isBefore(activation.getValidTo()))
                .orElse(false);
    }

    // 超级管理员发起服务费支付（模拟，金额500元/年）
    @Transactional
    public Result<String> payServiceFee(Long superAdminId) {
        // 模拟支付流程（参考学生支付逻辑）
        // 生成支付记录（金额500元，类型为系统服务费）
        // 此处简化为直接返回支付成功
        return Result.success("支付成功，请激活系统");
    }

    // 激活系统（生成秘钥+绑定设备）
    @Transactional
    public Result<SystemActivation> activateSystem(Long superAdminId, String deviceId) {
        // 1. 生成唯一秘钥
        String secretKey = "SYS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 2. 设置有效期（1年）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validTo = now.plusYears(1);

        // 3. 关闭之前的激活记录（确保唯一激活）
        activationRepository.findTopByIsActiveTrue().ifPresent(activation -> {
            activation.setActive(false);
            activationRepository.save(activation);
        });

        // 4. 创建新激活记录
        SystemActivation activation = new SystemActivation();
        activation.setSecretKey(secretKey);
        activation.setDeviceId(deviceId);
        activation.setValidFrom(now);
        activation.setValidTo(validTo);
        activation.setActive(true);
        activation.setSuperAdminId(superAdminId);

        SystemActivation saved = activationRepository.save(activation);
        return Result.success(saved);
    }

    /**
     * 新增：获取当前设备的激活过期信息
     * @param deviceId 设备唯一标识
     * @return 激活状态+到期时间
     */
    public Result<ActivationExpiryDTO> getActivationExpiry(String deviceId) {
        // 1. 查询当前设备的有效激活记录（isActive=true + deviceId匹配）
        Optional<SystemActivation> activeRecord = activationRepository
                .findByDeviceIdAndIsActive(deviceId, true);

        // 2. 封装返回VO
        ActivationExpiryDTO expiryVO = new ActivationExpiryDTO();
        if (activeRecord.isPresent()) {
            SystemActivation activation = activeRecord.get();
            expiryVO.setActive(true);
            // 时间格式化为：yyyy-MM-dd HH:mm:ss（便于前端解析）
            String validToStr = activation.getValidTo()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            expiryVO.setValidTo(validToStr);
        } else {
            // 无有效激活记录
            expiryVO.setActive(false);
            expiryVO.setValidTo(null);
        }

        return Result.success(expiryVO);
    }

    /**
     * 原有verifyDevice方法（确保正确，用于激活验证）
     * @param secretKey 激活秘钥
     * @param deviceId 设备ID
     * @return 验证结果
     */
    public Result<Boolean> verifyDevice(String secretKey, String deviceId) {
        Optional<SystemActivation> activation = activationRepository.findBySecretKey(secretKey);
        if (activation.isEmpty()) {
            return Result.error(StatusCode.FAIL, "激活记录不存在");
        }
        // 验证设备ID匹配 + 激活状态有效 + 未过期
        boolean isMatch = activation.get().getDeviceId().equals(deviceId)
                && activation.get().isActive()
                && LocalDateTime.now().isBefore(activation.get().getValidTo());
        return isMatch ? Result.success(true) : Result.error(StatusCode.FAIL, "设备验证失败");
    }

    // 新增：获取当前激活的记录
    public Optional<SystemActivation> getCurrentActiveActivation() {
        return activationRepository.findTopByIsActiveTrue();
    }
}
