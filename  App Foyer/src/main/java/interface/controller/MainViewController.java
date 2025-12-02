import backend.controller.DishController;
import backend.controller.OrderController;
import backend.controller.TicketController;
import backend.dto.DishRequestDTO;
import backend.dto.OrderRequestDTO;
import backend.dto.OrderResponseDTO;
import backend.entity.StatusOrder;
import backend.repository.impl.DishRepositoryImpl;
import backend.repository.impl.OrderRepositoryImpl;
import backend.service.DishService;
import backend.service.JavaxPrinterClient;
import backend.service.OrderService;
import backend.service.PlainTextTicketFormatter;
import backend.service.PrinterClient;
import backend.service.TicketFormatter;
import backend.service.TicketPrinterService;
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
    private OrderListController cancelledOrderListController;
    @FXML
    private DishGridController dishGridController;
    @FXML
    private OrderComposerController orderComposerController;
    @FXML
    private AnalyticsController analyticsController;
    @FXML
    private TabPane rootTabPane;
    @FXML
    private Tab newOrderTab;

    private OrderController orderController;
    private DishController dishController;
    private TicketController ticketController;

    @FXML
    public void initialize() {
        DishRepositoryImpl dishRepository = new DishRepositoryImpl();
        OrderRepositoryImpl orderRepository = new OrderRepositoryImpl();
        DishService dishService = new DishService(dishRepository);
        OrderService orderService = new OrderService(orderRepository, dishRepository);
        dishController = new DishController(dishService); // placeholder for future menu management
        orderController = new OrderController(orderService);
        purgeOldCancelledOrders();
        initializeTicketPrinter(orderRepository);
        if (analyticsController != null) {
            analyticsController.setOrderController(orderController);
            analyticsController.refreshAnalytics();
        }

        if (orderListController != null) {
            orderListController.setOrderController(orderController);
            orderListController.setAllowedStatuses(List.of(StatusOrder.ENCOURS));
            orderListController.setTimeFilterEnabled(false);
            orderListController.setTitle("Commandes actives");
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
                    handlePrintOrder(order);
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
            passedOrderListController.setTitle("Commandes terminées");
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
                    handlePrintOrder(order);
                }

                @Override
                public void onRecuperer(OrderResponseDTO order) {
                    handleRecupererOrder(order);
                }
            });
            passedOrderListController.refreshOrders();
        }
        if (cancelledOrderListController != null) {
            cancelledOrderListController.setOrderController(orderController);
            cancelledOrderListController.setAllowedStatuses(List.of(StatusOrder.ANNULER));
            cancelledOrderListController.setTimeFilterEnabled(true);
            cancelledOrderListController.setTitle("Commandes annulées");
            cancelledOrderListController.setOrderActionListener(new OrderListController.OrderActionListener() {
                @Override
                public void onCancel(OrderResponseDTO order) {
                    // already cancelled; no-op but keep consistent
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
                    handlePrintOrder(order);
                }

                @Override
                public void onRecuperer(OrderResponseDTO order) {
                    handleRecupererOrder(order);
                }
            });
            cancelledOrderListController.refreshOrders();
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
        statusLabel.setText("Prêt");
    }

    @FXML
    private void handleRefresh() {
        refreshOrderLists();
        statusLabel.setText("Commandes actualisées");
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
        }
        statusLabel.setText("Nouvelle commande démarrée");
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
                statusLabel.setText("Plat ajouté");
            } catch (RuntimeException exception) {
                showError("Impossible d'ajouter le plat", exception.getMessage());
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
                statusLabel.setText("Commande n°" + id + (request.isPayer() ? " créée et payée" : " créée"));
            } else {
                orderController.updateOrder(request);
                statusLabel.setText("Commande n°" + request.getOrderId() + (request.isPayer() ? " payée" : " mise à jour"));
            }
            refreshOrderLists();
            if (orderComposerController != null) {
                orderComposerController.startNewOrder();
            }
        } catch (RuntimeException exception) {
            showError("Impossible d'enregistrer la commande", exception.getMessage());
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
            statusLabel.setText("Commande n°" + order.getId() + " annulée");
        } catch (RuntimeException exception) {
            showError("Impossible d'annuler la commande", exception.getMessage());
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
            statusLabel.setText("Commande n°" + order.getId() + " marquée FINI");
        } catch (RuntimeException exception) {
            showError("Impossible de marquer comme terminée", exception.getMessage());
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

    private void handlePrintOrder(OrderResponseDTO order) {
        if (order == null) {
            return;
        }
        if (ticketController == null) {
            showError("Imprimante indisponible", "Aucune imprimante de tickets configurée. Veuillez en configurer une dans l'application.");
            return;
        }
        try {
            ticketController.printTicket(order.getId());
            statusLabel.setText("Ticket imprimé pour la commande n°" + order.getId());
        } catch (RuntimeException exception) {
            showError("Impossible d'imprimer le ticket", exception.getMessage());
        }
    }

    private void refreshOrderLists() {
        purgeOldCancelledOrders();
        if (orderListController != null) {
            orderListController.refreshOrders();
        }
        if (passedOrderListController != null) {
            passedOrderListController.refreshOrders();
        }
        if (cancelledOrderListController != null) {
            cancelledOrderListController.refreshOrders();
        }
        refreshAnalyticsView();
    }

    private void refreshAnalyticsView() {
        if (analyticsController != null) {
            analyticsController.refreshAnalytics();
        }
    }

    private void initializeTicketPrinter(OrderRepositoryImpl orderRepository) {
        try {
            TicketFormatter formatter = new PlainTextTicketFormatter("Commandes du restaurant");
            PrinterClient printerClient = new JavaxPrinterClient();
            TicketPrinterService printerService = new TicketPrinterService(orderRepository, formatter, printerClient);
            ticketController = new TicketController(printerService);
        } catch (RuntimeException exception) {
            ticketController = null;
            if (statusLabel != null) {
            statusLabel.setText("Imprimante indisponible : " + exception.getMessage());
            }
        }
    }

    private void purgeOldCancelledOrders() {
        if (orderController == null) {
            return;
        }
        try {
            orderController.purgeCancelledOlderThanDays(7);
        } catch (RuntimeException exception) {
            if (statusLabel != null) {
                statusLabel.setText("Impossible de purger les commandes annulées : " + exception.getMessage());
            }
        }
    }
}
