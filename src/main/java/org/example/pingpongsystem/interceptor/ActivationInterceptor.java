package org.example.pingpongsystem.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.pingpongsystem.service.SystemActivationService;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

@Component
public class ActivationInterceptor implements HandlerInterceptor {
    private final SystemActivationService activationService;
    private final ObjectMapper objectMapper;

    // 允许访问的接口（超级管理员激活相关）
    private static final List<String> ALLOWED_PATHS = Arrays.asList(
            "/super_admin/login",
            "/super_admin/pay_service_fee", // 支付接口必须允许
            "/super_admin/activate_system", // 激活接口必须允许
            "/super_admin/verify_activation",
            "/token/info"
    );

    public ActivationInterceptor(SystemActivationService activationService, ObjectMapper objectMapper) {
        this.activationService = activationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();

        // 1. 恢复：检查是否是允许的接口，是则直接放行（不校验激活）
        if (ALLOWED_PATHS.stream().anyMatch(requestPath::startsWith)) {
            return true; // 支付接口会在这里直接放行，不进入激活校验
        }

        // 2. 检查系统是否已激活
        if (!activationService.isSystemActivated()) {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write(objectMapper.writeValueAsString(Result.error(StatusCode.FAIL, "系统未激活，请联系超级管理员支付服务费并激活")));
            out.flush();
            out.close();
            return false;
        }

        // 3. 激活状态正常，允许访问
        return true;
    }
}