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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Root controller wires repository/service layer into the UI.
 */
public class MainViewController {
    private static final DateTimeFormatter ORDER_DETAILS_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.getDefault());
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
        if (rootTabPane != null && newOrderTab != null) {
            newOrderTab.setOnSelectionChanged(event -> {
                if (newOrderTab.isSelected() && orderComposerController != null) {
                    orderComposerController.startNewOrder();
                }
            });
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

    private void handleComposerSubmit(OrderRequestDTO request, boolean printTicket) {
        if (orderController == null) {
            return;
        }
        Long processedOrderId = null;
        try {
            if (request.getOrderId() == null) {
                long id = orderController.createOrder(request);
                processedOrderId = id;
                statusLabel.setText("Commande n°" + id + (request.isPayer() ? " créée et payée" : " créée"));
            } else {
                orderController.updateOrder(request);
                processedOrderId = request.getOrderId();
                statusLabel.setText("Commande n°" + request.getOrderId() + (request.isPayer() ? " payée" : " mise à jour"));
            }
            refreshOrderLists();
            if (orderComposerController != null) {
                orderComposerController.startNewOrder();
            }
            if (printTicket && processedOrderId != null) {
                printTicketAutomatically(processedOrderId);
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
        if (!showPrintConfirmation(order)) {
            if (statusLabel != null) {
                statusLabel.setText("Impression annulée pour la commande n°" + order.getId());
            }
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

    private void printTicketAutomatically(long orderId) {
        if (ticketController == null) {
            showError("Imprimante indisponible", "Aucune imprimante de tickets configurée. Veuillez en configurer une dans l'application.");
            return;
        }
        try {
            ticketController.printTicket(orderId);
            statusLabel.setText("Ticket imprimé pour la commande n°" + orderId);
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

    private boolean showPrintConfirmation(OrderResponseDTO order) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Imprimer la commande");
        alert.setHeaderText("Résumé de la commande n°" + order.getId());
        ButtonType printButton = new ButtonType("Imprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(printButton, cancelButton);

        TextArea summaryArea = new TextArea(buildOrderSummary(order));
        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.setPrefRowCount(Math.min(14, order.getItems().size() + 8));
        alert.getDialogPane().setContent(summaryArea);

        return alert.showAndWait().filter(response -> response == printButton).isPresent();
    }

    private String buildOrderSummary(OrderResponseDTO order) {
        StringBuilder builder = new StringBuilder();
        builder.append("Client : ").append(order.getCustomerName()).append('\n');
        if (order.getCreatedAt() != null) {
            builder.append("Créée le : ").append(ORDER_DETAILS_FORMATTER.format(order.getCreatedAt())).append('\n');
        }
        builder.append("Statut : ").append(order.getStatus() != null ? order.getStatus().name() : "Inconnu").append('\n');
        builder.append("Payée : ").append(order.isPayer() ? "Oui" : "Non").append('\n');
        if (order.getMessage() != null && !order.getMessage().isBlank()) {
            builder.append("Note : ").append(order.getMessage().trim()).append('\n');
        }
        builder.append("\nArticles :\n");
        order.getItems().forEach(item -> builder.append(String.format(Locale.getDefault(),
                "- %dx %s — %.2f €\n",
                item.getQuantity(),
                item.getDishName(),
                item.getPrice() * item.getQuantity())));
        builder.append("\nTotal : ").append(String.format(Locale.getDefault(), "%.2f €", order.getTotal()));
        return builder.toString();
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
