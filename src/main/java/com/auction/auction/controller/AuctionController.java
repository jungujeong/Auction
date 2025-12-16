package com.auction.auction.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auction.auction.model.AuctionParticipant;
import com.auction.auction.model.Bid;
import com.auction.auction.model.User;
import com.auction.auction.repository.UserRepository;
import com.auction.auction.service.AuctionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;
    private final UserRepository userRepository;

    /**
     * 경매 참여
     */
    @PostMapping("/{itemId}/join")
    public ResponseEntity<?> joinAuction(
            @PathVariable("itemId") Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            auctionService.joinAuction(itemId, user);
            return ResponseEntity.ok().body("경매 참여가 완료되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 내가 참여한 경매 목록
     */
    @GetMapping("/my-auctions")
    public ResponseEntity<List<AuctionParticipant>> getMyAuctions(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<AuctionParticipant> participants = auctionService.getUserAuctions(user.getId());
        return ResponseEntity.ok(participants);
    }

    /**
     * 특정 경매의 참여자 목록
     */
    @GetMapping("/{itemId}/participants")
    public ResponseEntity<List<AuctionParticipant>> getAuctionParticipants(@PathVariable("itemId") Long itemId) {
        List<AuctionParticipant> participants = auctionService.getAuctionParticipants(itemId);
        return ResponseEntity.ok(participants);
    }

    /**
     * 특정 경매의 입찰 내역
     */
    @GetMapping("/{itemId}/bids")
    public ResponseEntity<List<Bid>> getAuctionBids(@PathVariable("itemId") Long itemId) {
        List<Bid> bids = auctionService.getAuctionBids(itemId);
        return ResponseEntity.ok(bids);
    }

    /**
     * 입찰하기
     */
    @PostMapping("/{itemId}/bid")
    public ResponseEntity<?> placeBid(
            @PathVariable("itemId") Long itemId,
            @RequestBody BidRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            Bid bid = auctionService.placeBid(itemId, user, request.getBidAmount());
            return ResponseEntity.ok(bid);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DTO
    public static class BidRequest {
        private Long bidAmount;

        public Long getBidAmount() {
            return bidAmount;
        }

        public void setBidAmount(Long bidAmount) {
            this.bidAmount = bidAmount;
        }
    }
}
