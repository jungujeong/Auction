package com.auction.auction.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ItemResponse {
    private Long id;
    private String title;
    private String description;
    private Long startPrice;
    private Long currentPrice;
    private String imageUrl;
    private String status;
    private LocalDateTime recruitmentEndTime;
    private LocalDateTime auctionStartTime;
    private LocalDateTime endTime;
    private String sellerName;
    private LocalDateTime createdAt;
}
