import backend.controller.DishController;
import backend.controller.OrderController;
import backend.dto.DishRequestDTO;
import backend.dto.OrderRequestDTO;
import backend.dto.OrderResponseDTO;
import backend.entity.StatusOrder;
import backend.repository.impl.DishRepositoryImpl;
import backend.repository.impl.OrderRepositoryImpl;
import backend.service.DishService;
import backend.service.OrderService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

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
    private OrderListController passedOrderListController;
    @FXML
    private DishGridController dishGridController;
    @FXML
    private OrderComposerController orderComposerController;
    @FXML
    private TabPane rootTabPane;
    @FXML
    private Tab newOrderTab;

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
            orderListController.setAllowedStatuses(List.of(StatusOrder.ENCOURS));
            orderListController.setTimeFilterEnabled(false);
            orderListController.setTitle("Active Orders");
            orderListController.setOrderActionListener(new OrderListController.OrderActionListener() {
                @Override
                public void onCancel(OrderResponseDTO order) {
                    handleCancelOrder(order);
                }

                @Override
                public void onModify(OrderResponseDTO order) {
                    if (orderComposerController != null) {
                        if (rootTabPane != null && newOrderTab != null) {
                            rootTabPane.getSelectionModel().select(newOrderTab);
                        }
                        orderComposerController.editOrder(order);
                    }
                }

                @Override
                public void onPrint(OrderResponseDTO order) {
                    statusLabel.setText("Print for order #" + order.getId() + " not implemented");
                }

                @Override
                public void onRecuperer(OrderResponseDTO order) {
                    handleRecupererOrder(order);
                }
            });
            orderListController.refreshOrders();
        }
        if (passedOrderListController != null) {
            passedOrderListController.setOrderController(orderController);
            passedOrderListController.setAllowedStatuses(List.of(StatusOrder.FINI));
            passedOrderListController.setTimeFilterEnabled(true);
            passedOrderListController.setTitle("Passed Orders");
            passedOrderListController.setOrderActionListener(new OrderListController.OrderActionListener() {
                @Override
                public void onCancel(OrderResponseDTO order) {
                    handleCancelOrder(order);
                }

                @Override
                public void onModify(OrderResponseDTO order) {
                    if (orderComposerController != null) {
                        if (rootTabPane != null && newOrderTab != null) {
                            rootTabPane.getSelectionModel().select(newOrderTab);
                        }
                        orderComposerController.editOrder(order);
                    }
                }

                @Override
                public void onPrint(OrderResponseDTO order) {
                    statusLabel.setText("Print for order #" + order.getId() + " not implemented");
                }

                @Override
                public void onRecuperer(OrderResponseDTO order) {
                    handleRecupererOrder(order);
                }
            });
            passedOrderListController.refreshOrders();
        }
        if (dishGridController != null) {
            dishGridController.setDishController(dishController);
            dishGridController.refreshDishes();
        }
        if (orderComposerController != null) {
            orderComposerController.setDishController(dishController);
            orderComposerController.setOnSubmit(this::handleComposerSubmit);
            orderComposerController.startNewOrder();
        }
        statusLabel.setText("Ready");
    }

    @FXML
    private void handleRefresh() {
        refreshOrderLists();
        statusLabel.setText("Orders refreshed");
    }

    @FXML
    private void handleFilter() {
        if (orderListController != null) {
            orderListController.filterOrders(searchField.getText());
        }
        if (passedOrderListController != null) {
            passedOrderListController.filterOrders(searchField.getText());
        }
    }

    @FXML
    private void handleNewOrder() {
        if (rootTabPane != null && newOrderTab != null) {
            rootTabPane.getSelectionModel().select(newOrderTab);
        }
        if (orderComposerController != null) {
            orderComposerController.startNewOrder();
            statusLabel.setText("New order started");
        }
    }

    @FXML
    private void handleAddDish() {
        if (dishController == null) {
            return;
        }
        AddDishDialog.show().ifPresent(dto -> {
            try {
                dishController.addDish(dto);
                if (dishGridController != null) {
                    dishGridController.refreshDishes();
                }
                if (orderComposerController != null) {
                    orderComposerController.refreshDishes();
                }
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

    private void handleComposerSubmit(OrderRequestDTO request) {
        if (orderController == null) {
            return;
        }
        try {
            if (request.getOrderId() == null) {
                long id = orderController.createOrder(request);
                statusLabel.setText("Order #" + id + (request.isPayer() ? " created and paid" : " created"));
            } else {
                orderController.updateOrder(request);
                statusLabel.setText("Order #" + request.getOrderId() + (request.isPayer() ? " paid" : " updated"));
            }
            refreshOrderLists();
            if (orderComposerController != null) {
                orderComposerController.startNewOrder();
            }
        } catch (RuntimeException exception) {
            showError("Unable to save order", exception.getMessage());
        }
    }

    private void handleCancelOrder(OrderResponseDTO order) {
        if (orderController == null || order == null || order.getStatus() == StatusOrder.ANNULER) {
            return;
        }
        OrderRequestDTO request = buildUpdateRequest(order, StatusOrder.ANNULER);
        try {
            orderController.updateOrder(request);
            refreshOrderLists();
            statusLabel.setText("Order #" + order.getId() + " cancelled");
        } catch (RuntimeException exception) {
            showError("Unable to cancel order", exception.getMessage());
        }
    }

    private void handleRecupererOrder(OrderResponseDTO order) {
        if (orderController == null || order == null) {
            return;
        }
        OrderRequestDTO request = buildUpdateRequest(order, StatusOrder.FINI);
        request.setPayer(order.isPayer());
        try {
            orderController.updateOrder(request);
            refreshOrderLists();
            statusLabel.setText("Order #" + order.getId() + " marked as FINI");
        } catch (RuntimeException exception) {
            showError("Unable to mark as finished", exception.getMessage());
        }
    }

    private OrderRequestDTO buildUpdateRequest(OrderResponseDTO order, StatusOrder newStatus) {
        OrderRequestDTO request = new OrderRequestDTO();
        request.setOrderId(order.getId());
        request.setCustomerName(order.getCustomerName());
        request.setStatus(newStatus != null ? newStatus : order.getStatus());
        if (order.getMessage() != null && !order.getMessage().isBlank()) {
            request.setMessage(order.getMessage());
        }
        request.setPayer(order.isPayer());
        order.getItems().forEach(item -> {
            if (item.getDishId() <= 0) {
                return;
            }
            OrderRequestDTO.OrderItemRequest itemRequest = new OrderRequestDTO.OrderItemRequest();
            itemRequest.setDishId(item.getDishId());
            itemRequest.setQuantity(item.getQuantity());
            request.addItem(itemRequest);
        });
        return request;
    }

    private void refreshOrderLists() {
        if (orderListController != null) {
            orderListController.refreshOrders();
        }
        if (passedOrderListController != null) {
            passedOrderListController.refreshOrders();
        }
    }

}
