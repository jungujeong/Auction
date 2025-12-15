package com.auction.auction.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import com.auction.auction.model.Bid;
import com.auction.auction.model.User;
import com.auction.auction.repository.UserRepository;
import com.auction.auction.service.AuctionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
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
            @DestinationVariable Long itemId,
            BidRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            Bid bid = auctionService.placeBid(itemId, user, request.getBidAmount());

            // 입찰 성공 메시지 생성
            BidMessage message = new BidMessage();
            message.setItemId(itemId);
            message.setBidAmount(bid.getBidAmount());
            message.setBidderUsername(user.getUsername());
            message.setBidderName(user.getName());
            message.setBidTime(bid.getBidTime().toString());
            message.setSuccess(true);

            return message;
        } catch (Exception e) {
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
