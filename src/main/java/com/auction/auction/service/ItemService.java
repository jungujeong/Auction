package com.auction.auction.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.auction.model.Item;
import com.auction.auction.model.Item.ItemStatus;
import com.auction.auction.model.User;
import com.auction.auction.repository.ItemRepository;
import com.auction.auction.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    // 물건 등록
    @Transactional
    public Item registerItem(Item item, String username) {
        User seller = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        item.setSeller(seller);
        return itemRepository.save(item);
    }

    // 전체 목록 조회 (DELETED 제외)
    public List<Item> getAllItems() {
        return itemRepository.findByStatusNotOrderByCreatedAtDesc(ItemStatus.DELETED);
    }

    // 진행 중인 경매 목록 조회
    public List<Item> getActiveItems() {
        return itemRepository.findByStatusOrderByCreatedAtDesc(ItemStatus.ACTIVE);
    }

    // 상세 조회
    public Item getItem(Long id) {
        return itemRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("경매 물건을 찾을 수 없습니다."));
    }

    // 내가 등록한 물건 조회
    public List<Item> getMyItems(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return itemRepository.findBySellerId(user.getId());
    }

    // 물건 수정
    @Transactional
    public Item updateItem(Long itemId, Item updatedItem, String username) {
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("경매 물건을 찾을 수 없습니다."));

        // 판매자 본인인지 확인
        if (!item.getSeller().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인이 등록한 물건만 수정할 수 있습니다.");
        }

        // 수정 가능한 필드만 업데이트
        item.setTitle(updatedItem.getTitle());
        item.setDescription(updatedItem.getDescription());
        item.setStartPrice(updatedItem.getStartPrice());
        item.setEndTime(updatedItem.getEndTime());

        if (updatedItem.getImageUrl() != null) {
            item.setImageUrl(updatedItem.getImageUrl());
        }

        return itemRepository.save(item);
    }

    // 물건 삭제 (상태 변경)
    @Transactional
    public void deleteItem(Long itemId, String username) {
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("경매 물건을 찾을 수 없습니다."));

        // 판매자 본인인지 확인
        if (!item.getSeller().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인이 등록한 물건만 삭제할 수 있습니다.");
        }

        // 상태를 DELETED로 변경
        item.setStatus(ItemStatus.DELETED);
        itemRepository.save(item);
    }
}
