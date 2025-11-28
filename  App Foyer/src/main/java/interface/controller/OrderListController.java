import backend.controller.OrderController;
import backend.dto.OrderResponseDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class OrderListController {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.getDefault());

    @FXML
    private ListView<OrderResponseDTO> orderListView;

    @FXML
    private Label emptyStateLabel;

    private final ObservableList<OrderResponseDTO> visibleOrders = FXCollections.observableArrayList();
    private final List<OrderResponseDTO> allOrders = new ArrayList<>();
    private OrderController orderController;

    @FXML
    public void initialize() {
        orderListView.setItems(visibleOrders);
        orderListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OrderResponseDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("#%d %s (%s) %.2f$", item.getId(),
                            item.getCustomerName(), FORMATTER.format(item.getCreatedAt()), item.getTotal()));
                }
            }
        });
        updateEmptyState();
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    @FXML
    public void refreshOrders() {
        if (orderController == null) {
            return;
        }
        List<OrderResponseDTO> fetched = new ArrayList<>(orderController.listOrders());
        allOrders.clear();
        allOrders.addAll(fetched);
        visibleOrders.setAll(allOrders);
        updateEmptyState();
    }

    public void filterOrders(String filter) {
        if (filter == null || filter.isBlank()) {
            refreshOrders();
            return;
        }
        String normalized = filter.trim().toLowerCase(Locale.getDefault());
        List<OrderResponseDTO> filtered = allOrders.stream()
                .filter(order -> String.valueOf(order.getId()).contains(normalized)
                        || order.getCustomerName().toLowerCase(Locale.getDefault()).contains(normalized))
                .collect(Collectors.toList());
        visibleOrders.setAll(filtered);
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean empty = orderListView.getItems().isEmpty();
        emptyStateLabel.setVisible(empty);
        orderListView.setVisible(!empty);
    }
}
