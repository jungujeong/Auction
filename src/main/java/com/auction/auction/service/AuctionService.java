package com.auction.auction.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.auction.model.AuctionParticipant;
import com.auction.auction.model.Bid;
import com.auction.auction.model.Item;
import com.auction.auction.model.Item.ItemStatus;
import com.auction.auction.model.User;
import com.auction.auction.repository.AuctionParticipantRepository;
import com.auction.auction.repository.BidRepository;
import com.auction.auction.repository.ItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionParticipantRepository participantRepository;
    private final BidRepository bidRepository;
    private final ItemRepository itemRepository;

    /**
     * 경매 참여
     */
    @Transactional
    public void joinAuction(Long itemId, User user) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 물건입니다."));

        // 모집 중인지 확인
        if (item.getStatus() != ItemStatus.RECRUITING) {
            throw new IllegalStateException("현재 참여자를 모집하는 경매가 아닙니다.");
        }

        // 모집 종료 시간이 지났는지 확인
        if (LocalDateTime.now().isAfter(item.getRecruitmentEndTime())) {
            throw new IllegalStateException("모집 기간이 종료되었습니다.");
        }

        // 이미 참여했는지 확인
        if (participantRepository.existsByItemIdAndUserId(itemId, user.getId())) {
            throw new IllegalStateException("이미 참여한 경매입니다.");
        }

        // 판매자는 자신의 경매에 참여할 수 없음
        if (item.getSeller().getId().equals(user.getId())) {
            throw new IllegalStateException("자신이 등록한 물건의 경매에는 참여할 수 없습니다.");
        }

        // 참여자 등록
        AuctionParticipant participant = new AuctionParticipant();
        participant.setItem(item);
        participant.setUser(user);
        participantRepository.save(participant);
    }

    /**
     * 사용자가 참여한 경매 목록 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<AuctionParticipant> getUserAuctions(Long userId) {
        return participantRepository.findByUserIdOrderByJoinedAtDesc(userId);
    }

    /**
     * 경매방 나가기 (참여 취소)
     */
    @Transactional
    public void leaveAuction(Long itemId, Long userId) {
        AuctionParticipant participant = participantRepository.findByItemIdAndUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("참여하지 않은 경매입니다."));

        participantRepository.delete(participant);
    }

    /**
     * 특정 경매의 참여자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AuctionParticipant> getAuctionParticipants(Long itemId) {
        return participantRepository.findByItemId(itemId);
    }

    /**
     * 사용자가 특정 경매에 참여했는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isUserParticipant(Long itemId, Long userId) {
        return participantRepository.existsByItemIdAndUserId(itemId, userId);
    }

    /**
     * 입찰
     */
    @Transactional
    public Bid placeBid(Long itemId, User user, Long bidAmount) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 물건입니다."));

        // 경매 진행 중인지 확인
        if (item.getStatus() != ItemStatus.AUCTION_STARTED) {
            throw new IllegalStateException("현재 경매가 진행 중이 아닙니다.");
        }

        // 경매 종료 시간이 지났는지 확인
        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            throw new IllegalStateException("경매가 종료되었습니다.");
        }

        // 참여자인지 확인
        if (!participantRepository.existsByItemIdAndUserId(itemId, user.getId())) {
            throw new IllegalStateException("경매에 참여하지 않은 사용자입니다.");
        }

        // 입찰가가 현재가보다 높은지 확인
        if (bidAmount <= item.getCurrentPrice()) {
            throw new IllegalArgumentException("입찰가는 현재가보다 높아야 합니다.");
        }

        // 계좌 잔액 확인
        if (user.getBalance() < bidAmount) {
            throw new IllegalArgumentException("계좌 잔액이 부족합니다. (현재 잔액: " + user.getBalance() + "원)");
        }

        // 입찰 등록
        Bid bid = new Bid();
        bid.setItem(item);
        bid.setBidder(user);
        bid.setBidAmount(bidAmount);
        bidRepository.save(bid);

        // 현재가 업데이트
        item.setCurrentPrice(bidAmount);
        itemRepository.save(item);

        return bid;
    }

    /**
     * 특정 경매의 입찰 내역 조회
     */
    @Transactional(readOnly = true)
    public List<Bid> getAuctionBids(Long itemId) {
        return bidRepository.findByItemIdOrderByBidTimeDesc(itemId);
    }
}
