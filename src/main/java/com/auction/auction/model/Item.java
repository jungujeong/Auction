package com.auction.auction.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_price", nullable = false)
    private Long startPrice;

    @Column(name = "current_price", nullable = false)
    private Long currentPrice;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status = ItemStatus.RECRUITING;

    @Column(name = "recruitment_end_time", nullable = false)
    private LocalDateTime recruitmentEndTime;  // 참여자 모집 종료 시간 (등록 후 10분)

    @Column(name = "auction_start_time", nullable = false)
    private LocalDateTime auctionStartTime;    // 경매 시작 시간 (모집 종료와 동시)

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;             // 경매 종료 시간

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.currentPrice == null) {
            this.currentPrice = this.startPrice;
        }
        // 참여자 모집 종료 시간 = 등록 후 10분
        if (this.recruitmentEndTime == null) {
            this.recruitmentEndTime = this.createdAt.plusMinutes(10);
        }
        // 경매 시작 시간 = 모집 종료 시간
        if (this.auctionStartTime == null) {
            this.auctionStartTime = this.recruitmentEndTime;
        }
    }

    public enum ItemStatus {
        RECRUITING,        // 참여자 모집 중
        AUCTION_STARTED,   // 경매 진행 중
        AUCTION_ENDED,     // 경매 종료
        SOLD,              // 판매 완료
        DELETED            // 삭제됨
    }
}
