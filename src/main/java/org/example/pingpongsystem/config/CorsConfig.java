package org.example.pingpongsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration // 标记为配置类，让 Spring 自动加载
public class CorsConfig {

    // 定义一个 CORS 过滤器 Bean，全局生效
    @Bean
    public CorsFilter corsFilter() {
        // 1. 配置跨域核心参数
        CorsConfiguration config = new CorsConfiguration();

        // 允许的前端域名（生产环境必须指定具体域名，禁止用 *）
        // 例：允许 https://your-frontend.com 和 http://localhost:8080 访问
        config.addAllowedOrigin("http://localhost:9528");
        config.addAllowedOrigin("http://localhost:9528");

        // 允许携带 Cookie（前后端都需要配置，否则跨域请求会丢失 Cookie）
        config.setAllowCredentials(true);

        // 允许的 HTTP 方法（GET/POST/PUT/DELETE 等，* 表示所有）
        config.addAllowedMethod("*");

        // 允许的请求头（如 Content-Type、Authorization 等，* 表示所有）
        config.addAllowedHeader("*");

        // 预检请求（OPTIONS）的有效期（秒），有效期内不再重复发送预检请求
        config.setMaxAge(3600L); // 1小时

        // 2. 配置路径映射（指定哪些接口需要跨域设置）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口生效（/** 表示匹配所有路径）
        source.registerCorsConfiguration("/**", config);

        // 3. 返回过滤器实例
        return new CorsFilter(source);
    }
}
