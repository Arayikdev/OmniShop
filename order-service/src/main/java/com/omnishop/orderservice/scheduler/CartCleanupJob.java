package com.omnishop.orderservice.scheduler;

import com.omnishop.orderservice.repository.CartItemRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CartCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(CartCleanupJob.class);

    private final CartItemRepository cartItemRepository;

    public CartCleanupJob(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "cartCleanup", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void cleanup() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            cartItemRepository.deleteByAddedAtBefore(cutoff);
            log.info("Cart cleanup completed");
        } catch (Exception e) {
            log.error("CartCleanupJob error: {}", e.getMessage(), e);
        }
    }
}
