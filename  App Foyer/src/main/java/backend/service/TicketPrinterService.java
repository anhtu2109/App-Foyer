package backend.service;

import backend.entity.Order;
import backend.repository.OrderRepository;

/**
 * Coordinates fetching an order, formatting it, and printing the ticket.
 */
public class TicketPrinterService {
    private final OrderRepository orderRepository;
    private final TicketFormatter formatter;
    private final PrinterClient printerClient;

    public TicketPrinterService(OrderRepository orderRepository,
                                TicketFormatter formatter,
                                PrinterClient printerClient) {
        this.orderRepository = orderRepository;
        this.formatter = formatter;
        this.printerClient = printerClient;
    }

    public void printTicket(long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        byte[] payload = formatter.format(order);
        printerClient.print(payload);
    }
}

