package backend.service;

import backend.entity.Order;
import backend.entity.OrderItem;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Basic formatter that produces a plain-text receipt.
 */
public class PlainTextTicketFormatter implements TicketFormatter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final String restaurantName;

    public PlainTextTicketFormatter(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    @Override
    public byte[] format(Order order) {
        StringBuilder builder = new StringBuilder();
        builder.append("=== ").append(restaurantName).append(" ===\n");
        builder.append("Commande n°").append(order.getId() == null ? "-" : order.getId()).append("\n");
        builder.append("Client : ").append(order.getCustomerName() == null ? "N/D" : order.getCustomerName()).append("\n");
        builder.append("Statut : ").append(order.getStatus()).append("\n");
        builder.append("Créée : ").append(order.getCreatedAt() == null ? "-" : DATE_FORMAT.format(order.getCreatedAt())).append("\n");
        builder.append("--------------------------------\n");
        for (OrderItem item : order.getItems()) {
            builder.append(item.getQuantity())
                    .append(" x ")
                    .append(item.getDish().getName())
                    .append(" @ ")
                    .append(String.format("%.2f", item.getPrice()))
                    .append("\n");
        }
        builder.append("--------------------------------\n");
        builder.append("Total : ").append(String.format("%.2f", order.getTotal())).append("\n");
        if (order.getMessage() != null && !order.getMessage().isBlank()) {
            builder.append("Note : ").append(order.getMessage()).append("\n");
        }
        builder.append(order.isPayer() ? "PAYÉ" : "NON PAYÉ").append("\n\n");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
