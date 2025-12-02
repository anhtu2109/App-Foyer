import backend.controller.DishController;
import backend.dto.DishResponseDTO;
import backend.dto.OrderRequestDTO;
import backend.dto.OrderResponseDTO;
import backend.entity.DishCategory;
import backend.entity.StatusOrder;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class OrderComposerController {
    @FXML
    private TilePane dishTilePane;
    @FXML
    private ListView<OrderLine> orderItemsListView;
    @FXML
    private Label emptyOrderLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private TextField customerNameField;
    @FXML
    private TextArea messageField;
    @FXML
    private Label modeLabel;
    @FXML
    private Button commanderButton;
    @FXML
    private Button payerButton;
    @FXML
    private Button resetButton;
    @FXML
    private ToggleGroup categoryToggleGroup;

    private final List<DishResponseDTO> availableDishes = new ArrayList<>();
    private final ObservableList<OrderLine> orderLines = FXCollections.observableArrayList();
    private final Map<Long, OrderLine> linesByDish = new HashMap<>();
    private DishController dishController;
    private final BooleanProperty formInvalid = new SimpleBooleanProperty(true);
    private Consumer<OrderRequestDTO> submitHandler = request -> { };
    private Long editingOrderId;
    private boolean payerFlag;
    private DishCategory activeCategoryFilter;

    @FXML
    public void initialize() {
        dishTilePane.setHgap(8);
        dishTilePane.setVgap(8);
        dishTilePane.setPrefColumns(3);
        orderItemsListView.setItems(orderLines);
        orderItemsListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OrderLine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format(Locale.getDefault(), "%s  x%d  —  %.2f €",
                            item.getDish().getName(), item.getQuantity(), item.getLineTotal()));
                }
            }
        });
        orderLines.addListener((ListChangeListener<OrderLine>) change -> updateSummary());
        if (customerNameField != null) {
            customerNameField.textProperty().addListener((obs, oldValue, newValue) -> updateFormValidity());
        }
        if (commanderButton != null) {
            commanderButton.disableProperty().bind(formInvalid);
        }
        if (payerButton != null) {
            payerButton.disableProperty().bind(formInvalid);
        }
        startNewOrder();
    }

    public void setDishController(DishController dishController) {
        this.dishController = dishController;
        populateDishGrid();
    }

    public void refreshDishes() {
        populateDishGrid();
    }

    public void setOnSubmit(Consumer<OrderRequestDTO> handler) {
        this.submitHandler = handler != null ? handler : request -> { };
    }

    @FXML
    private void handleReset() {
        startNewOrder();
    }

    @FXML
    private void handleCommanderAction() {
        submit(StatusOrder.ENCOURS, payerFlag);
    }

    @FXML
    private void handlePayerAction() {
        payerFlag = true;
        submit(StatusOrder.ENCOURS, true);
    }

    public void startNewOrder() {
        editingOrderId = null;
        payerFlag = false;
        updateModeLabel(false);
        if (customerNameField != null) {
            customerNameField.clear();
        }
        if (messageField != null) {
            messageField.clear();
        }
        orderLines.clear();
        linesByDish.clear();
        if (orderItemsListView != null) {
            orderItemsListView.refresh();
        }
        updateSummary();
    }

    public void editOrder(OrderResponseDTO order) {
        if (order == null) {
            startNewOrder();
            return;
        }
        editingOrderId = order.getId();
        payerFlag = order.isPayer();
        updateModeLabel(true);
        if (customerNameField != null) {
            customerNameField.setText(order.getCustomerName());
        }
        if (messageField != null) {
            messageField.setText(order.getMessage());
        }
        orderLines.clear();
        linesByDish.clear();
        for (OrderResponseDTO.OrderItemResponse item : order.getItems()) {
            DishResponseDTO dish = resolveDish(item.getDishId(), item.getDishName(), item.getPrice());
            if (dish == null) {
                continue;
            }
            OrderLine line = new OrderLine(dish);
            line.setQuantity(item.getQuantity());
            orderLines.add(line);
            if (dish.getId() != null) {
                linesByDish.put(dish.getId(), line);
            }
        }
        orderItemsListView.refresh();
        updateSummary();
    }

    private void updateModeLabel(boolean editing) {
        if (modeLabel != null) {
            String label = editing ? "Editing order #" + (editingOrderId != null ? editingOrderId : "?") : "Creating new order";
            modeLabel.setText(label);
        }
    }

    private DishResponseDTO resolveDish(long dishId, String fallbackName, double fallbackPrice) {
        if (dishController == null) {
            return null;
        }
        Optional<DishResponseDTO> existing = dishController.getDish(dishId);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (dishId <= 0) {
            return null;
        }
        DishResponseDTO placeholder = new DishResponseDTO();
        placeholder.setId(dishId);
        placeholder.setName(fallbackName);
        placeholder.setPrice(fallbackPrice);
        placeholder.setCategory(DishCategory.ENTREE);
        return placeholder;
    }

    private void populateDishGrid() {
        if (dishController == null || dishTilePane == null) {
            return;
        }
        availableDishes.clear();
        availableDishes.addAll(dishController.getMenu());
        renderDishCards();
    }

    private void renderDishCards() {
        if (dishTilePane == null) {
            return;
        }
        dishTilePane.getChildren().clear();
        availableDishes.stream()
                .filter(dish -> activeCategoryFilter == null || dish.getCategory() == activeCategoryFilter)
                .map(this::createDishCard)
                .forEach(node -> dishTilePane.getChildren().add(node));
    }

    private Node createDishCard(DishResponseDTO dish) {
        Label name = new Label(dish.getName());
        name.getStyleClass().add("order-dish-title");

        Label price = new Label(String.format(Locale.getDefault(), "%.2f €", dish.getPrice()));
        price.getStyleClass().add("order-dish-price");

        VBox content = new VBox(6, name, price);
        content.getStyleClass().add("order-dish-card");
        StackPane wrapper = new StackPane(content);
        wrapper.getStyleClass().add("order-dish-card-wrapper");
        if (dish.getCategory() != null) {
            String categoryClass = "category-" + dish.getCategory().name().toLowerCase(Locale.ROOT);
            wrapper.getStyleClass().add(categoryClass);
        }
        wrapper.setPrefSize(140, 90);
        wrapper.setOnMouseClicked(event -> handleDishSelected(dish));
        return wrapper;
    }

    private void handleDishSelected(DishResponseDTO dish) {
        if (dish.getId() == null) {
            return;
        }
        OrderLine line = linesByDish.computeIfAbsent(dish.getId(), id -> {
            OrderLine created = new OrderLine(dish);
            orderLines.add(created);
            return created;
        });
        line.increment();
        orderItemsListView.refresh();
        updateSummary();
    }

    private void submit(StatusOrder status, boolean payer) {
        OrderRequestDTO dto = buildRequest(status, payer);
        submitHandler.accept(dto);
    }

    @FXML
    private void handleCategoryFilter(ActionEvent event) {
        if (categoryToggleGroup == null) {
            return;
        }
        Toggle selected = categoryToggleGroup.getSelectedToggle();
        if (selected == null || selected.getUserData() == null || "ALL".equals(selected.getUserData())) {
            activeCategoryFilter = null;
        } else {
            try {
                activeCategoryFilter = DishCategory.valueOf(selected.getUserData().toString());
            } catch (IllegalArgumentException ex) {
                activeCategoryFilter = null;
            }
        }
        renderDishCards();
    }

    private OrderRequestDTO buildRequest(StatusOrder status, boolean payer) {
        if (isFormInvalid()) {
            throw new IllegalStateException("Order form is incomplete");
        }
        OrderRequestDTO dto = new OrderRequestDTO();
        dto.setOrderId(editingOrderId);
        dto.setCustomerName(customerNameField.getText().trim());
        dto.setStatus(status);
        String note = messageField.getText();
        if (note != null && !note.trim().isEmpty()) {
            dto.setMessage(note.trim());
        } else {
            dto.setMessage(null);
        }
        dto.setPayer(payer);
        for (OrderLine line : orderLines) {
            if (line.getDish().getId() == null) {
                continue;
            }
            OrderRequestDTO.OrderItemRequest itemRequest = new OrderRequestDTO.OrderItemRequest();
            itemRequest.setDishId(line.getDish().getId());
            itemRequest.setQuantity(line.getQuantity());
            dto.addItem(itemRequest);
        }
        return dto;
    }

    private void updateSummary() {
        double total = orderLines.stream().mapToDouble(OrderLine::getLineTotal).sum();
        totalLabel.setText(String.format(Locale.getDefault(), "%.2f €", total));
        boolean hasItems = !orderLines.isEmpty();
        orderItemsListView.setVisible(hasItems);
        orderItemsListView.setManaged(hasItems);
        emptyOrderLabel.setVisible(!hasItems);
        emptyOrderLabel.setManaged(!hasItems);
        updateFormValidity();
    }

    private void updateFormValidity() {
        formInvalid.set(isFormInvalid());
    }

    private boolean isFormInvalid() {
        String customerName = customerNameField != null ? customerNameField.getText() : null;
        boolean nameMissing = customerName == null || customerName.trim().isEmpty();
        return nameMissing || orderLines.isEmpty();
    }

    public BooleanProperty formInvalidProperty() {
        return formInvalid;
    }

    private static class OrderLine {
        private final DishResponseDTO dish;
        private int quantity;

        OrderLine(DishResponseDTO dish) {
            this.dish = dish;
        }

        void increment() {
            quantity++;
        }

        void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        DishResponseDTO getDish() {
            return dish;
        }

        int getQuantity() {
            return quantity;
        }

        double getLineTotal() {
            return dish.getPrice() * quantity;
        }
    }
}
