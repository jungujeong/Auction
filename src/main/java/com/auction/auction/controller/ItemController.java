package com.auction.auction.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auction.auction.dto.ItemRequest;
import com.auction.auction.dto.ItemResponse;
import com.auction.auction.model.Item;
import com.auction.auction.service.ItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // 물건 등록
    @PostMapping
    public ResponseEntity<?> registerItem(
            @Valid @RequestBody ItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Item item = new Item();
            item.setTitle(request.getTitle());
            item.setDescription(request.getDescription());
            item.setStartPrice(request.getStartPrice());
            item.setImageUrl(request.getImageUrl());
            item.setEndTime(request.getEndTime());

            Item savedItem = itemService.registerItem(item, userDetails.getUsername());

            ItemResponse response = convertToResponse(savedItem);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("물건 등록 실패: " + e.getMessage());
        }
    }

    // 전체 목록 조회
    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        List<Item> items = itemService.getAllItems();
        List<ItemResponse> response = items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 진행 중인 경매만 조회
    @GetMapping("/active")
    public ResponseEntity<List<ItemResponse>> getActiveItems() {
        List<Item> items = itemService.getActiveItems();
        List<ItemResponse> response = items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getItem(@PathVariable("id") Long id) {
        try {
            Item item = itemService.getItem(id);
            ItemResponse response = convertToResponse(item);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 내가 등록한 물건 조회
    @GetMapping("/my")
    public ResponseEntity<List<ItemResponse>> getMyItems(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<Item> items = itemService.getMyItems(userDetails.getUsername());
        List<ItemResponse> response = items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 물건 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(
            @PathVariable("id") Long id,
            @Valid @RequestBody ItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Item item = new Item();
            item.setTitle(request.getTitle());
            item.setDescription(request.getDescription());
            item.setStartPrice(request.getStartPrice());
            item.setImageUrl(request.getImageUrl());
            item.setEndTime(request.getEndTime());

            Item updatedItem = itemService.updateItem(id, item, userDetails.getUsername());

            ItemResponse response = convertToResponse(updatedItem);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("물건 수정 실패: " + e.getMessage());
        }
    }

    // 물건 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            itemService.deleteItem(id, userDetails.getUsername());
            return ResponseEntity.ok("물건이 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Entity -> DTO 변환
    private ItemResponse convertToResponse(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getStartPrice(),
                item.getCurrentPrice(),
                item.getImageUrl(),
                item.getStatus().name(),
                item.getRecruitmentEndTime(),
                item.getAuctionStartTime(),
                item.getEndTime(),
                item.getSeller().getUsername(),
                item.getCreatedAt()
        );
    }
}
