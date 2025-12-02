package backend.service;

import backend.entity.Order;

/**
 * Converts an order into a printable payload.
 */
public interface TicketFormatter {
    byte[] format(Order order);
}

