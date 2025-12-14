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

    // 전체 목록 조회
    public List<Item> getAllItems() {
        return itemRepository.findAllByOrderByCreatedAtDesc();
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
}
