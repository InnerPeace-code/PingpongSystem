package org.example.pingpongsystem.controller;

import lombok.Data;
import org.example.pingpongsystem.entity.CoachEntity;
import org.example.pingpongsystem.entity.CoachTeachStudentEntity;
import org.example.pingpongsystem.entity.StudentEntity;
import org.example.pingpongsystem.service.CoachService;
import org.example.pingpongsystem.service.StudentService;
import org.example.pingpongsystem.service.TokenService;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.interfaces.InfoAns;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/token")
@RestController
public class TokenController {
    private final TokenService tokenService;
    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/info")
    public Result<InfoAns> updateInfo(@RequestParam(name = "token") String token) {
        return tokenService.getInfo(token);
    }

    @PostMapping("/logout") // 匹配前端的url: '/token/logout'
    public Result<String> logout(@RequestBody LogoutRequest request) { // 用@RequestBody接收请求体中的token
        System.out.println(request);
        return tokenService.logout(request.token);
    }

    @Data
    private static class LogoutRequest {
        private String token;
    }
}
