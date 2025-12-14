package com.auction.auction.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auction.auction.dto.LoginRequest;
import com.auction.auction.dto.LoginResponse;
import com.auction.auction.dto.SignupRequest;
import com.auction.auction.dto.UserResponse;
import com.auction.auction.model.User;
import com.auction.auction.service.UserService;
import com.auction.auction.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            // DTO -> Entity 변환
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setEmail(request.getEmail());
            user.setName(request.getName());

            // 회원가입 처리
            User savedUser = userService.registerUser(user);

            // Entity -> DTO 변환 (비밀번호 제외)
            UserResponse response = new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getCreatedAt()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Spring Security 인증 토큰 생성
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

            // 인증 수행 (CustomUserDetailsService와 PasswordEncoder 사용)
            Authentication authentication = authenticationManager.authenticate(authToken);

            // 사용자 정보 조회
            User user = userService.login(request.getUsername(), request.getPassword());

            // JWT 토큰 생성
            String token = jwtUtil.generateToken(user.getUsername());

            // Entity -> DTO 변환 (JWT 토큰 포함)
            LoginResponse response = new LoginResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("로그인 실패: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT 방식에서는 클라이언트에서 토큰을 삭제하면 됨
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}
