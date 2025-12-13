package com.auction.auction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스는 모두 허용
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                // 회원가입, 로그인 API는 모두 허용
                .requestMatchers("/api/users/signup", "/api/users/login").permitAll()
                // 홈, 경매 목록, 로그인, 회원가입 페이지는 모두 허용
                .requestMatchers("/", "/items.html", "/login.html", "/signup.html").permitAll()
                // 물건 등록, 경매 상세(입찰) 페이지는 로그인 필요
                .requestMatchers("/register-item.html", "/auction-detail.html").authenticated()
                // 나머지 API는 인증 필요
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .permitAll()
                .disable() // 기본 폼 로그인 비활성화 (우리는 REST API 사용)
            )
            .logout(logout -> logout
                .logoutUrl("/api/users/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable()); // REST API 사용을 위해 CSRF 비활성화

        return http.build();
    }
}
