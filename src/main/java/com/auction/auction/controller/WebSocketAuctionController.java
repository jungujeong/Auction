package com.auction.auction.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.auction.auction.model.Bid;
import com.auction.auction.model.User;
import com.auction.auction.repository.UserRepository;
import com.auction.auction.service.AuctionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuctionController {

    private final AuctionService auctionService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 입찰 메시지 처리
     * 클라이언트가 /app/auction/{itemId}/bid 로 메시지를 보내면
     * /topic/auction/{itemId} 를 구독하는 모든 클라이언트에게 입찰 정보를 전송
     */
    @MessageMapping("/auction/{itemId}/bid")
    @SendTo("/topic/auction/{itemId}")
    public BidMessage handleBid(
            @DestinationVariable("itemId") Long itemId,
            @Payload BidRequest request,
            Principal principal) {

        log.info("==== WebSocket 입찰 요청 수신 ====");
        log.info("물건 ID: {}", itemId);
        log.info("입찰 금액: {}", request != null ? request.getBidAmount() : "null");
        log.info("사용자: {}", principal != null ? principal.getName() : "null");

        try {
            if (principal == null) {
                log.error("인증 정보가 없습니다 (principal is null)");
                throw new IllegalStateException("로그인이 필요합니다.");
            }

            if (request == null || request.getBidAmount() == null) {
                log.error("입찰 금액이 없습니다 (request or bidAmount is null)");
                throw new IllegalArgumentException("입찰 금액을 입력해주세요.");
            }

            String username = principal.getName();
            log.info("사용자 조회 중: {}", username);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            log.info("입찰 처리 시작 - 사용자: {}, 금액: {}", user.getUsername(), request.getBidAmount());
            Bid bid = auctionService.placeBid(itemId, user, request.getBidAmount());

            log.info("입찰 성공! 입찰 ID: {}, 금액: {}", bid.getId(), bid.getBidAmount());

            // 입찰 성공 메시지 생성
            BidMessage message = new BidMessage();
            message.setItemId(itemId);
            message.setBidAmount(bid.getBidAmount());
            message.setBidderUsername(user.getUsername());
            message.setBidderName(user.getName());
            message.setBidTime(bid.getBidTime().toString());
            message.setSuccess(true);

            log.info("입찰 메시지 브로드캐스트 준비 완료");
            log.info("====================================");
            return message;
        } catch (Exception e) {
            log.error("==== 입찰 실패 ====");
            log.error("예외 타입: {}", e.getClass().getName());
            log.error("오류 메시지: {}", e.getMessage());
            log.error("스택 트레이스:", e);
            log.error("====================");

            // 입찰 실패 메시지 생성
            BidMessage errorMessage = new BidMessage();
            errorMessage.setItemId(itemId);
            errorMessage.setSuccess(false);
            errorMessage.setErrorMessage(e.getMessage());
            return errorMessage;
        }
    }

    // 요청 DTO
    public static class BidRequest {
        private Long bidAmount;

        public Long getBidAmount() {
            return bidAmount;
        }

        public void setBidAmount(Long bidAmount) {
            this.bidAmount = bidAmount;
        }
    }

    // 응답 DTO
    public static class BidMessage {
        private Long itemId;
        private Long bidAmount;
        private String bidderUsername;
        private String bidderName;
        private String bidTime;
        private boolean success;
        private String errorMessage;

        // Getters and Setters
        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Long getBidAmount() {
            return bidAmount;
        }

        public void setBidAmount(Long bidAmount) {
            this.bidAmount = bidAmount;
        }

        public String getBidderUsername() {
            return bidderUsername;
        }

        public void setBidderUsername(String bidderUsername) {
            this.bidderUsername = bidderUsername;
        }

        public String getBidderName() {
            return bidderName;
        }

        public void setBidderName(String bidderName) {
            this.bidderName = bidderName;
        }

        public String getBidTime() {
            return bidTime;
        }

        public void setBidTime(String bidTime) {
            this.bidTime = bidTime;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
