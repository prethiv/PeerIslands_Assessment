package com.peerisland.ecomm.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Periodically advances active orders through their lifecycle so the demo
 * reflects fulfilment progress without manual intervention:
 * PENDING -> PROCESSING -> SHIPPED -> DELIVERED.
 *
 * <p>Cancelled and delivered orders are terminal and left untouched.
 * Scheduling is enabled via {@code @EnableScheduling} on the application class.
 */
@Component
public class OrderScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderScheduler.class);

    private final OrderRepository orderRepository;

    public OrderScheduler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedRateString = "${ecomm.scheduler.advance-rate-ms:30000}")
    @Transactional
    public void advanceOrders() {
        List<Order> active = orderRepository.findByStatus(OrderStatus.PENDING);
        active.addAll(orderRepository.findByStatus(OrderStatus.PROCESSING));
        active.addAll(orderRepository.findByStatus(OrderStatus.SHIPPED));

        if (active.isEmpty()) {
            return;
        }

        for (Order order : active) {
            OrderStatus next = nextStatus(order.getStatus());
            if (next != null) {
                log.info("Advancing order {} from {} to {}", order.getId(), order.getStatus(), next);
                order.setStatus(next);
                order.setUpdatedAt(Instant.now());
            }
        }
        orderRepository.saveAll(active);
    }

    private OrderStatus nextStatus(OrderStatus current) {
        return switch (current) {
            case PENDING -> OrderStatus.PROCESSING;
            case PROCESSING -> OrderStatus.SHIPPED;
            case SHIPPED -> OrderStatus.DELIVERED;
            default -> null;
        };
    }
}
