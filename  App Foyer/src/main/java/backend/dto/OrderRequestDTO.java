package backend.dto;

import backend.entity.StatusOrder;

import java.util.ArrayList;
import java.util.List;

public class OrderRequestDTO {
    private Long orderId;
    private String customerName;
    private StatusOrder status;
    private String message;
    private boolean payer;
    private final List<OrderItemRequest> items = new ArrayList<>();

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void addItem(OrderItemRequest item) {
        this.items.add(item);
    }

    public static class OrderItemRequest {
        private long dishId;
        private int quantity;

        public OrderItemRequest() {
        }

        public OrderItemRequest(long dishId, int quantity) {
            this.dishId = dishId;
            this.quantity = quantity;
        }

        public long getDishId() {
            return dishId;
        }

        public void setDishId(long dishId) {
            this.dishId = dishId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}
