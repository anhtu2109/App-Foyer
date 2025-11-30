import backend.controller.OrderController;
import backend.dto.OrderResponseDTO;
import backend.entity.StatusOrder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderListController {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.getDefault());

    @FXML
    private ListView<OrderResponseDTO> orderListView;

    @FXML
    private Label emptyStateLabel;
    @FXML
    private Label titleLabel;

    @FXML
    private ComboBox<String> timeFilterComboBox;

    private final ObservableList<OrderResponseDTO> visibleOrders = FXCollections.observableArrayList();
    private final List<OrderResponseDTO> allOrders = new ArrayList<>();
    private OrderController orderController;
    private OrderActionListener actionListener;
    private final List<StatusOrder> allowedStatuses = new ArrayList<>();
    private boolean timeFilterEnabled;
    private TimeFilter currentTimeFilter = TimeFilter.ALL;
    private String searchTerm = "";

    @FXML
    public void initialize() {
        orderListView.setItems(visibleOrders);
        orderListView.setCellFactory(list -> new OrderCell());
        if (timeFilterComboBox != null) {
            setupTimeFilterCombo();
            setTimeFilterEnabled(false);
        }
        updateEmptyState();
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    public void setOrderActionListener(OrderActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setAllowedStatuses(List<StatusOrder> statuses) {
        allowedStatuses.clear();
        if (statuses != null) {
            allowedStatuses.addAll(statuses);
        }
        applyFilters();
    }

    public void setTimeFilterEnabled(boolean enabled) {
        this.timeFilterEnabled = enabled;
        if (timeFilterComboBox != null) {
            timeFilterComboBox.setVisible(enabled);
            timeFilterComboBox.setManaged(enabled);
            if (!enabled) {
                currentTimeFilter = TimeFilter.ALL;
                timeFilterComboBox.getSelectionModel().select("All");
            }
        }
        applyFilters();
    }

    public void setTitle(String title) {
        if (titleLabel != null && title != null) {
            titleLabel.setText(title);
        }
    }

    @FXML
    public void refreshOrders() {
        if (orderController == null) {
            return;
        }
        List<OrderResponseDTO> fetched = new ArrayList<>(orderController.listOrders());
        allOrders.clear();
        allOrders.addAll(fetched);
        applyFilters();
    }

    public void filterOrders(String filter) {
        if (filter == null) {
            searchTerm = "";
        } else {
            searchTerm = filter.trim().toLowerCase(Locale.getDefault());
        }
        applyFilters();
    }

    private void updateEmptyState() {
        boolean empty = orderListView.getItems().isEmpty();
        emptyStateLabel.setVisible(empty);
        orderListView.setVisible(!empty);
    }

    private void setupTimeFilterCombo() {
        timeFilterComboBox.getItems().setAll("All", "Today", "Last 1 Month", "Last 3 Months");
        timeFilterComboBox.getSelectionModel().selectFirst();
        timeFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                currentTimeFilter = TimeFilter.ALL;
            } else {
                currentTimeFilter = switch (newValue) {
                    case "Today" -> TimeFilter.TODAY;
                    case "Last 1 Month" -> TimeFilter.ONE_MONTH;
                    case "Last 3 Months" -> TimeFilter.THREE_MONTHS;
                    default -> TimeFilter.ALL;
                };
            }
            applyFilters();
        });
    }

    private void applyFilters() {
        List<OrderResponseDTO> filtered = allOrders.stream()
                .filter(this::matchesStatusFilter)
                .filter(this::matchesSearchFilter)
                .filter(this::matchesTimeFilter)
                .collect(Collectors.toList());
        visibleOrders.setAll(filtered);
        updateEmptyState();
    }

    private boolean matchesStatusFilter(OrderResponseDTO order) {
        if (allowedStatuses.isEmpty()) {
            return true;
        }
        return order.getStatus() != null && allowedStatuses.contains(order.getStatus());
    }

    private boolean matchesSearchFilter(OrderResponseDTO order) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return true;
        }
        return String.valueOf(order.getId()).toLowerCase(Locale.getDefault()).contains(searchTerm)
                || order.getCustomerName().toLowerCase(Locale.getDefault()).contains(searchTerm);
    }

    private boolean matchesTimeFilter(OrderResponseDTO order) {
        if (!timeFilterEnabled || currentTimeFilter == TimeFilter.ALL) {
            return true;
        }
        LocalDateTime createdAt = order.getCreatedAt();
        if (createdAt == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        return switch (currentTimeFilter) {
            case TODAY -> createdAt.toLocalDate().isEqual(LocalDate.now());
            case ONE_MONTH -> !createdAt.isBefore(now.minusMonths(1));
            case THREE_MONTHS -> !createdAt.isBefore(now.minusMonths(3));
            default -> true;
        };
    }

    private class OrderCell extends ListCell<OrderResponseDTO> {
        private final Label titleLabel = new Label();
        private final Label subtitleLabel = new Label();
        private final Label totalLabel = new Label();
        private final Label statusBadge = new Label();
        private final Button cancelButton = new Button("Annuler");
        private final Button modifyButton = new Button("Modifier");
        private final Button printButton = new Button("Imprimer");
        private final Button recupererButton = new Button("Récupérer");
        private final HBox header = new HBox(10);
        private final HBox buttonRow = new HBox(8);
        private final VBox container = new VBox(6);

        private OrderCell() {
            header.setSpacing(10);
            VBox metaBox = new VBox(titleLabel, subtitleLabel);
            subtitleLabel.getStyleClass().add("muted-label");
            header.getChildren().addAll(metaBox, statusBadge, totalLabel);
            HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);
            statusBadge.getStyleClass().add("status-badge");
            totalLabel.getStyleClass().add("order-total");
            buttonRow.getChildren().addAll(cancelButton, modifyButton, printButton, recupererButton);
            container.getStyleClass().add("order-card");
            container.getChildren().addAll(header, buttonRow);
            cancelButton.getStyleClass().add("ghost-button");
            modifyButton.getStyleClass().add("ghost-button");
            printButton.getStyleClass().add("ghost-button");
            recupererButton.getStyleClass().add("ghost-button");
        }

        @Override
        protected void updateItem(OrderResponseDTO order, boolean empty) {
            super.updateItem(order, empty);
            if (empty || order == null) {
                setGraphic(null);
                return;
            }
            titleLabel.setText(String.format("#%d %s", order.getId(), order.getCustomerName()));
            StringBuilder subtitle = new StringBuilder(String.format("%s • %.2f €",
                    FORMATTER.format(order.getCreatedAt()), order.getTotal()));
            if (order.isPayer()) {
                subtitle.append(" • Payé");
            }
            subtitleLabel.setText(subtitle.toString());
            updateStatusBadge(order.getStatus());
            totalLabel.setText(String.format(Locale.getDefault(), "%.2f €", order.getTotal()));
            cancelButton.setDisable(order.getStatus() == StatusOrder.ANNULER);
            cancelButton.setOnAction(evt -> {
                if (actionListener != null) {
                    actionListener.onCancel(order);
                }
            });
            modifyButton.setOnAction(evt -> {
                if (actionListener != null) {
                    actionListener.onModify(order);
                }
            });
            printButton.setOnAction(evt -> {
                if (actionListener != null) {
                    actionListener.onPrint(order);
                }
            });
            recupererButton.setDisable(order.getStatus() == StatusOrder.FINI);
            recupererButton.setOnAction(evt -> {
                if (actionListener != null) {
                    actionListener.onRecuperer(order);
                }
            });
            setGraphic(container);
        }

        private void updateStatusBadge(StatusOrder status) {
            statusBadge.setText(status != null ? status.name() : "");
            statusBadge.getStyleClass().removeAll("status-annuler", "status-encours", "status-fini");
            if (status == null) {
                return;
            }
            switch (status) {
                case ANNULER -> statusBadge.getStyleClass().add("status-annuler");
                case FINI -> statusBadge.getStyleClass().add("status-fini");
                default -> statusBadge.getStyleClass().add("status-encours");
            }
        }
    }

    public interface OrderActionListener {
        void onCancel(OrderResponseDTO order);

        void onModify(OrderResponseDTO order);

        void onPrint(OrderResponseDTO order);

        void onRecuperer(OrderResponseDTO order);
    }

    private enum TimeFilter {
        ALL,
        TODAY,
        ONE_MONTH,
        THREE_MONTHS
    }
}
