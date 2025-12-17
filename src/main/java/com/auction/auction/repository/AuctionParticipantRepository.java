package com.auction.auction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.auction.auction.model.AuctionParticipant;

@Repository
public interface AuctionParticipantRepository extends JpaRepository<AuctionParticipant, Long> {

    // 특정 경매의 모든 참여자 조회
    List<AuctionParticipant> findByItemId(Long itemId);

    // 특정 사용자가 참여한 모든 경매 조회 (최신순 - 최근 참여한 것이 위로)
    List<AuctionParticipant> findByUserIdOrderByJoinedAtDesc(Long userId);

    // 특정 사용자가 특정 경매에 참여했는지 확인
    Optional<AuctionParticipant> findByItemIdAndUserId(Long itemId, Long userId);

    // 특정 사용자가 특정 경매에 참여했는지 여부 확인
    boolean existsByItemIdAndUserId(Long itemId, Long userId);

    // 특정 경매의 참여자 수 조회
    long countByItemId(Long itemId);
}
