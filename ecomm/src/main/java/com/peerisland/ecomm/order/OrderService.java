package com.peerisland.ecomm.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Business logic for creating, querying and transitioning orders.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Persists a new order. The order always starts in {@link OrderStatus#PENDING}
     * and the back-reference from each item to its parent order is wired up.
     */
    @Transactional
    public Order createOrder(Order order) {
        order.setId(null);
        order.setStatus(OrderStatus.PENDING);
        Instant now = Instant.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        // Re-attach items through the helper so the relationship is owned correctly,
        // even though Jackson populated the list directly during deserialization.
        List<OrderItem> incoming = List.copyOf(order.getItems());
        order.getItems().clear();
        for (OrderItem item : incoming) {
            order.addItem(item);
        }

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(OrderStatus status) {
        return status == null
                ? orderRepository.findAll()
                : orderRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * Cancels an order. Only orders that are still {@link OrderStatus#PENDING}
     * can be cancelled.
     */
    @Transactional
    public Order cancelOrder(Long id) {
        Order order = getOrder(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalOrderStateException(
                    "Only PENDING orders can be cancelled (order " + id + " is " + order.getStatus() + ")");
        }
        return transitionTo(order, OrderStatus.CANCELLED);
    }

    /**
     * Moves an order to an explicit status.
     */
    @Transactional
    public Order updateStatus(Long id, OrderStatus newStatus) {
        Order order = getOrder(id);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalOrderStateException(
                    "Cannot change status of a " + order.getStatus() + " order (order " + id + ")");
        }
        return transitionTo(order, newStatus);
    }

    private Order transitionTo(Order order, OrderStatus status) {
        order.setStatus(status);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }
}
