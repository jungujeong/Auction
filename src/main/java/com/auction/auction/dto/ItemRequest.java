package com.auction.auction.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemRequest {
    private String title;
    private String description;
    private Long startPrice;
    private String imageUrl;
    private LocalDateTime endTime;
}
