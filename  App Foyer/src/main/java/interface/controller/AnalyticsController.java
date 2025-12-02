import backend.controller.OrderController;
import backend.dto.OrderResponseDTO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Controller displaying analytics charts and KPIs for orders.
 */
public class AnalyticsController {
    private static final DateTimeFormatter DATE_LABEL_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault());

    @FXML
    private ComboBox<String> periodFilterComboBox;
    @FXML
    private LineChart<String, Number> revenueChart;
    @FXML
    private PieChart dishPieChart;
    @FXML
    private Label totalOrdersLabel;
    @FXML
    private Label revenueLabel;
    @FXML
    private Label averageTicketLabel;

    private OrderController orderController;
    private PeriodFilter currentFilter = PeriodFilter.ALL;

    @FXML
    public void initialize() {
        if (periodFilterComboBox != null) {
            periodFilterComboBox.setItems(FXCollections.observableArrayList(
                    PeriodFilter.ALL.display,
                    PeriodFilter.ONE_DAY.display,
                    PeriodFilter.ONE_MONTH.display,
                    PeriodFilter.THREE_MONTHS.display,
                    PeriodFilter.ONE_YEAR.display
            ));
            periodFilterComboBox.getSelectionModel().selectFirst();
            periodFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
                currentFilter = PeriodFilter.fromDisplay(newValue);
                refreshAnalytics();
            });
        }
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    @FXML
    private void handleRefresh() {
        refreshAnalytics();
    }

    public void refreshAnalytics() {
        if (orderController == null) {
            clearUi();
            return;
        }
        List<OrderResponseDTO> orders = orderController.listOrders();
        List<OrderResponseDTO> filtered = orders.stream()
                .filter(this::matchesFilter)
                .filter(OrderResponseDTO::isPayer)
                .collect(Collectors.toList());
        updateRevenueChart(filtered);
        updateDishPieChart(filtered);
        updateStats(filtered);
    }

    private void updateRevenueChart(List<OrderResponseDTO> orders) {
        revenueChart.getData().clear();
        if (orders.isEmpty()) {
            return;
        }
        Map<LocalDate, Double> totalsByDate = new TreeMap<>();
        for (OrderResponseDTO order : orders) {
            LocalDate date = order.getCreatedAt() != null
                    ? order.getCreatedAt().toLocalDate()
                    : LocalDate.now();
            totalsByDate.merge(date, order.getTotal(), Double::sum);
        }
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        totalsByDate.forEach((date, total) ->
                series.getData().add(new XYChart.Data<>(DATE_LABEL_FORMAT.format(date), total)));
        series.setName("Chiffre d'affaires");
        revenueChart.getData().add(series);
    }

    private void updateDishPieChart(List<OrderResponseDTO> orders) {
        dishPieChart.getData().clear();
        if (orders.isEmpty()) {
            return;
        }
        Map<String, Double> revenueByDish = orders.stream()
                .flatMap(order -> order.getItems() != null ? order.getItems().stream() : Stream.empty())
                .collect(Collectors.groupingBy(
                        OrderResponseDTO.OrderItemResponse::getDishName,
                        Collectors.summingDouble(item -> item.getPrice() * item.getQuantity())
                ));
        dishPieChart.setData(FXCollections.observableArrayList(
                revenueByDish.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList())
        ));
    }

    private void updateStats(List<OrderResponseDTO> orders) {
        int totalOrders = orders.size();
        double revenue = orders.stream().mapToDouble(OrderResponseDTO::getTotal).sum();
        double averageTicket = totalOrders > 0 ? revenue / totalOrders : 0.0;
        totalOrdersLabel.setText(String.valueOf(totalOrders));
        revenueLabel.setText(String.format(Locale.getDefault(), "%.2f €", revenue));
        averageTicketLabel.setText(String.format(Locale.getDefault(), "%.2f €", averageTicket));
    }

    private boolean matchesFilter(OrderResponseDTO order) {
        if (order.getCreatedAt() == null || currentFilter == PeriodFilter.ALL) {
            return currentFilter == PeriodFilter.ALL || order.getCreatedAt() != null;
        }
        LocalDateTime createdAt = order.getCreatedAt();
        LocalDateTime now = LocalDateTime.now();
        return switch (currentFilter) {
            case ONE_DAY -> !createdAt.isBefore(now.minusDays(1));
            case ONE_MONTH -> !createdAt.isBefore(now.minusMonths(1));
            case THREE_MONTHS -> !createdAt.isBefore(now.minusMonths(3));
            case ONE_YEAR -> !createdAt.isBefore(now.minusYears(1));
            default -> true;
        };
    }

    private void clearUi() {
        revenueChart.getData().clear();
        dishPieChart.getData().clear();
        totalOrdersLabel.setText("-");
        revenueLabel.setText("-");
        averageTicketLabel.setText("-");
    }

    private enum PeriodFilter {
        ALL("Tous"),
        ONE_DAY("1 jour"),
        ONE_MONTH("1 mois"),
        THREE_MONTHS("3 mois"),
        ONE_YEAR("1 an");

        private final String display;

        PeriodFilter(String display) {
            this.display = display;
        }

        private static PeriodFilter fromDisplay(String value) {
            if (value == null) {
                return ALL;
            }
            for (PeriodFilter filter : values()) {
                if (filter.display.equals(value)) {
                    return filter;
                }
            }
            return ALL;
        }
    }
}
