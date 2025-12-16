package com.auction.auction.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.auction.auction.model.Item;
import com.auction.auction.model.Item.ItemStatus;
import com.auction.auction.repository.ItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionStatusScheduler {

    private final ItemRepository itemRepository;

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

        // AUCTION_STARTED -> AUCTION_ENDED
        List<Item> activeItems = itemRepository.findByStatus(ItemStatus.AUCTION_STARTED);
        for (Item item : activeItems) {
            if (now.isAfter(item.getEndTime())) {
                item.setStatus(ItemStatus.AUCTION_ENDED);
                itemRepository.save(item);
                log.info("경매 종료: 물건 ID = {}, 제목 = {}", item.getId(), item.getTitle());
            }
        }
    }
}
