package backend.service;

import backend.dto.OrderRequestDTO;
import backend.dto.OrderResponseDTO;
import backend.entity.Dish;
import backend.entity.Order;
import backend.entity.OrderItem;
import backend.repository.DishRepository;
import backend.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderService {
    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;

    public OrderService(OrderRepository orderRepository, DishRepository dishRepository) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
    }

    public List<OrderResponseDTO> getOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Optional<OrderResponseDTO> getOrder(long orderId) {
        return orderRepository.findById(orderId).map(this::mapToResponse);
    }

    public long createOrder(OrderRequestDTO request) {
        Order order = mapToEntity(request);
        long id = orderRepository.create(order);
        order.setId(id);
        return id;
    }

    public void updateOrder(OrderRequestDTO request) {
        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("Order id is required for updates");
        }
        Order order = mapToEntity(request);
        order.setId(request.getOrderId());
        orderRepository.update(order);
    }

    public void deleteOrder(long id) {
        orderRepository.delete(id);
    }

    private Order mapToEntity(OrderRequestDTO request) {
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setStatus(request.getStatus() != null ? request.getStatus() : "NEW");
        order.setCreatedAt(LocalDateTime.now());
        List<OrderItem> items = request.getItems().stream()
                .map(itemRequest -> {
                    Dish dish = dishRepository.findById(itemRequest.getDishId())
                            .orElseThrow(() -> new IllegalArgumentException("Dish not found: " + itemRequest.getDishId()));
                    OrderItem orderItem = new OrderItem();
                    orderItem.setDish(dish);
                    orderItem.setQuantity(itemRequest.getQuantity());
                    orderItem.setPrice(dish.getPrice());
                    return orderItem;
                })
                .collect(Collectors.toList());
        order.setItems(items);
        return order;
    }

    private OrderResponseDTO mapToResponse(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setCustomerName(order.getCustomerName());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setTotal(order.getTotal());
        order.getItems().forEach(orderItem -> {
            OrderResponseDTO.OrderItemResponse itemResponse = new OrderResponseDTO.OrderItemResponse();
            if (orderItem.getId() != null) {
                itemResponse.setItemId(orderItem.getId());
            }
            itemResponse.setDishName(orderItem.getDish().getName());
            itemResponse.setQuantity(orderItem.getQuantity());
            itemResponse.setPrice(orderItem.getPrice());
            dto.addItem(itemResponse);
        });
        return dto;
    }
}
