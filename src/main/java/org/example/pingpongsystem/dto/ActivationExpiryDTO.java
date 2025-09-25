package org.example.pingpongsystem.dto;


import lombok.Data;

// 系统激活过期信息VO
@Data
public class ActivationExpiryDTO {
    // 激活状态（true=已激活，false=未激活）
    private boolean isActive;
    // 到期时间（格式：yyyy-MM-dd HH:mm:ss，便于前端解析）
    private String validTo;
}