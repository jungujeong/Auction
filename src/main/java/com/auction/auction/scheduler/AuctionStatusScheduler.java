package com.auction.auction.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.auction.auction.model.Bid;
import com.auction.auction.model.Item;
import com.auction.auction.model.Item.ItemStatus;
import com.auction.auction.model.User;
import com.auction.auction.repository.BidRepository;
import com.auction.auction.repository.ItemRepository;
import com.auction.auction.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionStatusScheduler {

    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    /**
     * 매 10초마다 경매 상태 자동 업데이트
     * - RECRUITING -> AUCTION_STARTED (모집 종료 시간 지남)
     * - AUCTION_STARTED -> AUCTION_ENDED (경매 종료 시간 지남)
     */
    @Scheduled(fixedRate = 10000) // 10초마다 실행 (10000ms)
    @Transactional
    public void updateAuctionStatus() {
        LocalDateTime now = LocalDateTime.now();

        // RECRUITING -> AUCTION_STARTED
        List<Item> recruitingItems = itemRepository.findByStatus(ItemStatus.RECRUITING);
        for (Item item : recruitingItems) {
            if (item.getRecruitmentEndTime() != null && now.isAfter(item.getRecruitmentEndTime())) {
                item.setStatus(ItemStatus.AUCTION_STARTED);
                itemRepository.save(item);
                log.info("경매 시작: 물건 ID = {}, 제목 = {}", item.getId(), item.getTitle());
            }
        }

        // AUCTION_STARTED -> AUCTION_ENDED (낙찰 처리 포함)
        List<Item> activeItems = itemRepository.findByStatus(ItemStatus.AUCTION_STARTED);
        for (Item item : activeItems) {
            if (now.isAfter(item.getEndTime())) {
                item.setStatus(ItemStatus.AUCTION_ENDED);

                // 낙찰자 결정 및 금액 차감
                List<Bid> bids = bidRepository.findByItemIdOrderByBidTimeDesc(item.getId());
                if (!bids.isEmpty()) {
                    // 가장 최근(마지막) 입찰자가 낙찰자
                    Bid winningBid = bids.get(0);
                    User winner = winningBid.getBidder();

                    // 낙찰자 설정
                    item.setWinnerId(winner.getId());

                    // 낙찰자 계좌에서 금액 차감
                    Long finalPrice = item.getCurrentPrice();
                    if (winner.getBalance() >= finalPrice) {
                        winner.setBalance(winner.getBalance() - finalPrice);
                        userRepository.save(winner);
                        log.info("낙찰 완료: 물건 ID = {}, 제목 = {}, 낙찰자 = {}, 낙찰가 = {}원",
                                item.getId(), item.getTitle(), winner.getUsername(), finalPrice);
                    } else {
                        log.warn("낙찰자 잔액 부족: 물건 ID = {}, 낙찰자 = {}, 필요 금액 = {}원, 현재 잔액 = {}원",
                                item.getId(), winner.getUsername(), finalPrice, winner.getBalance());
                    }
                }

                itemRepository.save(item);
                log.info("경매 종료: 물건 ID = {}, 제목 = {}", item.getId(), item.getTitle());
            }
        }
    }
}
