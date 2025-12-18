package com.auction.auction.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.auction.auction.model.Item;
import com.auction.auction.model.Item.ItemStatus;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // 상태별 조회
    List<Item> findByStatus(ItemStatus status);

    // 판매자별 조회
    List<Item> findBySellerId(Long sellerId);

    // 최신순 조회 (DELETED 제외)
    List<Item> findByStatusNotOrderByCreatedAtDesc(ItemStatus status);

    // 진행 중인 경매 최신순 조회
    List<Item> findByStatusOrderByCreatedAtDesc(ItemStatus status);
}
