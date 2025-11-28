import backend.controller.DishController;
import backend.controller.OrderController;
import backend.dto.DishRequestDTO;
import backend.repository.impl.DishRepositoryImpl;
import backend.repository.impl.OrderRepositoryImpl;
import backend.service.DishService;
import backend.service.OrderService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Root controller wires repository/service layer into the UI.
 */
public class MainViewController {
    @FXML
    private TextField searchField;

    @FXML
    private Label statusLabel;

    @FXML
    private OrderListController orderListController;
    @FXML
    private DishGridController dishGridController;
    private OrderController orderController;
    private DishController dishController;

    @FXML
    public void initialize() {
        DishRepositoryImpl dishRepository = new DishRepositoryImpl();
        OrderRepositoryImpl orderRepository = new OrderRepositoryImpl();
        DishService dishService = new DishService(dishRepository);
        OrderService orderService = new OrderService(orderRepository, dishRepository);
        dishController = new DishController(dishService); // placeholder for future menu management
        orderController = new OrderController(orderService);

        if (orderListController != null) {
            orderListController.setOrderController(orderController);
            orderListController.refreshOrders();
        }
        if (dishGridController != null) {
            dishGridController.setDishController(dishController);
            dishGridController.refreshDishes();
        }
        statusLabel.setText("Ready");
    }

    @FXML
    private void handleRefresh() {
        if (orderListController != null) {
            orderListController.refreshOrders();
            statusLabel.setText("Orders refreshed");
        }
    }

    @FXML
    private void handleFilter() {
        if (orderListController != null) {
            orderListController.filterOrders(searchField.getText());
        }
    }

    @FXML
    private void handleNewOrder() {
        statusLabel.setText("Implement order creation dialog");
    }

    @FXML
    private void handleAddDish() {
        if (dishController == null) {
            return;
        }
        AddDishDialog.show().ifPresent(dto -> {
            try {
                dishController.addDish(dto);
                statusLabel.setText("Dish added");
            } catch (RuntimeException exception) {
                showError("Unable to add dish", exception.getMessage());
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
