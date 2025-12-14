package com.auction.auction.config;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** 경로로 접근 시 실제 파일 시스템의 uploads/images 폴더로 매핑
        // context-path가 /auction이므로 실제 접근 경로는 /auction/uploads/**가 됨
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toString().replace("\\", "/");
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations("file:///" + absolutePath + "/");
    }
}
