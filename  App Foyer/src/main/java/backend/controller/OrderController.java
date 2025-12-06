package backend.controller;

import backend.dto.OrderRequestDTO;
import backend.dto.OrderResponseDTO;
import backend.service.OrderService;

import java.util.List;
import java.util.Optional;

public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public List<OrderResponseDTO> listOrders() {
        return orderService.getOrders();
    }

    public Optional<OrderResponseDTO> getOrder(long id) {
        return orderService.getOrder(id);
    }

    public long createOrder(OrderRequestDTO request) {
        return orderService.createOrder(request);
    }

    public void updateOrder(OrderRequestDTO request) {
        orderService.updateOrder(request);
    }

    public void deleteOrder(long id) {
        orderService.deleteOrder(id);
    }

    public void purgeCancelledOlderThanDays(int days) {
        orderService.purgeCancelledOlderThanDays(days);
    }
}
