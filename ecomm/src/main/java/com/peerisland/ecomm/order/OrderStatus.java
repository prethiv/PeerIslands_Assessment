package com.peerisland.ecomm.order;

/**
 * Lifecycle states an {@link Order} can be in.
 *
 * <p>The normal progression is PENDING -> PROCESSING -> SHIPPED -> DELIVERED.
 * An order may be CANCELLED while it is still PENDING.
 */
public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
