package org.example.pingpongsystem.config;

import org.example.pingpongsystem.utility.Utility;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // 配置静态资源映射：将本地图片目录映射为URL路径
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地图片存储路径（从Utility中获取）
        String coachPhotoPath = Utility.CoachPhotoPath.replace("\\", "/");
        System.out.println("file:" + coachPhotoPath + "/");
        // 映射规则：前端访问 /coach-photos/** 路径时，实际访问本地CoachPhoto目录
        registry.addResourceHandler("/coach-photos/**")
                .addResourceLocations("file:" + coachPhotoPath + "/");
    }

    // 配置跨域：允许前端9528端口访问
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 所有接口都允许跨域
                .allowedOrigins("http://localhost:9528") // 前端地址
                .allowedMethods("GET", "POST", "PUT", "DELETE") // 允许的请求方法
                .allowedHeaders("*") // 允许的请求头
                .allowCredentials(true); // 允许携带cookie
    }
}
