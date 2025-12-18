package com.auction.auction.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * WebSocket 메시지 처리 중 발생하는 예외 처리
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleWebSocketException(Exception e) {
        log.error("==== WebSocket 오류 발생 ====");
        log.error("예외 타입: {}", e.getClass().getName());
        log.error("오류 메시지: {}", e.getMessage());
        log.error("스택 트레이스:", e);
        log.error("================================");
        return "오류가 발생했습니다: " + e.getMessage();
    }

    /**
     * 인증 관련 예외 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        log.error("==== 인증 오류 ====");
        log.error("오류 메시지: {}", ex.getMessage());
        log.error("스택 트레이스:", ex);
        log.error("==================");

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "인증에 실패했습니다: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 권한 관련 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("==== 권한 오류 ====");
        log.error("오류 메시지: {}", ex.getMessage());
        log.error("스택 트레이스:", ex);
        log.error("==================");

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "권한이 없습니다: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Bean Validation 검증 실패 시 호출
     * @Valid 어노테이션으로 검증 실패 시 MethodArgumentNotValidException 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        log.warn("==== Validation 오류 ====");
        log.warn("검증 실패 필드 수: {}", ex.getBindingResult().getErrorCount());

        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        // 모든 필드 에러를 순회하며 에러 메시지 수집
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
            log.warn("필드: {}, 오류: {}", fieldName, errorMessage);
        });

        response.put("success", false);
        response.put("message", "입력값 검증에 실패했습니다");
        response.put("errors", errors);

        log.warn("==========================");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * IllegalArgumentException 처리 (비즈니스 로직 검증 실패)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        log.error("==== IllegalArgumentException ====");
        log.error("오류 메시지: {}", ex.getMessage());
        log.error("스택 트레이스:", ex);
        log.error("==================================");

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * IllegalStateException 처리 (상태 오류)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException ex) {
        log.error("==== IllegalStateException ====");
        log.error("오류 메시지: {}", ex.getMessage());
        log.error("스택 트레이스:", ex);
        log.error("================================");

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 일반적인 예외 처리 (Catch-all)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("==== 예외 발생 (Catch-all) ====");
        log.error("예외 타입: {}", ex.getClass().getName());
        log.error("오류 메시지: {}", ex.getMessage());
        log.error("스택 트레이스:", ex);
        log.error("================================");

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "서버에서 오류가 발생했습니다: " + ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
