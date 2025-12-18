package com.auction.auction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.auction.auction.model.Bid;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    // 특정 경매의 모든 입찰 내역 조회 (최신순)
    List<Bid> findByItemIdOrderByBidTimeDesc(Long itemId);

    // 특정 경매의 최고 입찰 조회
    @Query("SELECT b FROM Bid b WHERE b.item.id = :itemId ORDER BY b.bidAmount DESC LIMIT 1")
    Optional<Bid> findTopBidByItemId(Long itemId);

    // 특정 경매의 입찰 개수
    long countByItemId(Long itemId);

    // 특정 사용자의 입찰 내역
    List<Bid> findByBidderIdOrderByBidTimeDesc(Long bidderId);

    // 특정 경매의 특정 사용자 입찰 내역
    List<Bid> findByItemIdAndBidderIdOrderByBidTimeDesc(Long itemId, Long bidderId);
}
