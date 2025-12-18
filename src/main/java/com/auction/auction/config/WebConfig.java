package com.auction.auction.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * LocaleResolver 설정
     * 세션에 언어 정보를 저장하고, 기본 언어는 한국어로 설정
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREAN);
        return resolver;
    }

    /**
     * LocaleChangeInterceptor 설정
     * URL 파라미터 'lang'으로 언어 변경 가능 (예: ?lang=en, ?lang=ko)
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/images/** 경로로 접근 시 실제 파일 시스템의 uploads/images 폴더로 매핑
        // context-path가 /auction이므로 실제 접근 경로는 /auction/uploads/images/**가 됨

        // classpath 기준 상대 경로 또는 file: 프로토콜 사용
        // file:./ 는 애플리케이션 실행 디렉토리 기준 상대 경로
        String resourceLocation = "file:./" + uploadDir + "/";

        log.info("==== Static Resource Mapping ====");
        log.info("Upload Directory: {}", uploadDir);
        log.info("Resource Location: {}", resourceLocation);
        log.info("Handler Pattern: /uploads/images/**");
        log.info("Working Directory: {}", System.getProperty("user.dir"));
        log.info("=================================");

        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(resourceLocation);

        // WebJars 리소스 매핑 (SockJS, STOMP 등)
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations(
                    "classpath:/META-INF/resources/webjars/",
                    "classpath:/webjars/")
                .resourceChain(false);
    }

    /**
     * Interceptor 등록
     * LocaleChangeInterceptor를 추가하여 언어 변경 처리
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
