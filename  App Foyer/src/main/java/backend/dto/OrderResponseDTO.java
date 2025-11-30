package backend.dto;

import backend.entity.StatusOrder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderResponseDTO {
    private Long id;
    private String customerName;
    private StatusOrder status;
    private LocalDateTime createdAt;
    private double total;
    private String message;
    private boolean payer;
    private final List<OrderItemResponse> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public StatusOrder getStatus() {
        return status;
    }

    public void setStatus(StatusOrder status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void addItem(OrderItemResponse response) {
        this.items.add(response);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isPayer() {
        return payer;
    }

    public void setPayer(boolean payer) {
        this.payer = payer;
    }

    public static class OrderItemResponse {
        private long itemId;
        private long dishId;
        private String dishName;
        private int quantity;
        private double price;

        public long getDishId() {
            return dishId;
        }

        public void setDishId(long dishId) {
            this.dishId = dishId;
        }

        public long getItemId() {
            return itemId;
        }

        public void setItemId(long itemId) {
            this.itemId = itemId;
        }

        public String getDishName() {
            return dishName;
        }

        public void setDishName(String dishName) {
            this.dishName = dishName;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }
}
