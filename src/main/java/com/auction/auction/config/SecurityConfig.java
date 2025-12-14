package com.auction.auction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.auction.auction.filter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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
            .csrf(csrf -> csrf.disable()) // JWT 사용으로 CSRF 비활성화
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 사용 안 함 (JWT 기반)
            )
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스는 모두 허용
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                // 회원가입, 로그인 API는 모두 허용
                .requestMatchers("/api/users/signup", "/api/users/login").permitAll()
                // 모든 HTML 페이지는 접근 허용 (JWT는 JavaScript에서 확인)
                .requestMatchers("/**/*.html", "/").permitAll()
                // API는 인증 필요
                .requestMatchers("/api/**").authenticated()
                // 나머지는 허용
                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable()) // 폼 로그인 비활성화
            .httpBasic(basic -> basic.disable()) // HTTP Basic 비활성화
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 필터 추가

        return http.build();
    }
}
