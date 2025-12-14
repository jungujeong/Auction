package com.auction.auction.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(min = 2, max = 100, message = "제목은 2~100자여야 합니다")
    private String title;

    @NotBlank(message = "설명은 필수입니다")
    @Size(min = 10, max = 1000, message = "설명은 10~1000자여야 합니다")
    private String description;

    @NotNull(message = "시작 가격은 필수입니다")
    @Min(value = 1000, message = "시작 가격은 최소 1000원 이상이어야 합니다")
    private Long startPrice;

    private String imageUrl;

    @NotNull(message = "경매 종료 시간은 필수입니다")
    @Future(message = "경매 종료 시간은 현재 시간 이후여야 합니다")
    private LocalDateTime endTime;
}
