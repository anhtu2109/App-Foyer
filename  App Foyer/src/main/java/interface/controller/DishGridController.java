import backend.controller.DishController;
import backend.dto.DishResponseDTO;
import backend.entity.DishCategory;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class DishGridController {
    @FXML
    private VBox categoryContainer;

    @FXML
    private Label emptyStateLabel;
    @FXML
    private Button deleteModeButton;

    private DishController dishController;
    private boolean deleteModeEnabled;
    private static final String DELETE_PASSWORD = "2109";

    @FXML
    public void initialize() {
        categoryContainer.setFillWidth(true);
        updateDeleteModeButton();
    }

    public void setDishController(DishController dishController) {
        this.dishController = dishController;
    }

    @FXML
    public void refreshDishes() {
        if (dishController == null) {
            return;
        }
        List<DishResponseDTO> dishes = dishController.getMenu();
        Map<DishCategory, List<DishResponseDTO>> grouped = groupByCategory(dishes);
        List<Node> sections = new ArrayList<>();
        grouped.forEach((category, dishList) -> {
            if (dishList.isEmpty()) {
                return;
            }
            Label header = new Label(category.name());
            header.getStyleClass().add("dish-category-title");

            javafx.scene.layout.TilePane tilePane = new javafx.scene.layout.TilePane();
            tilePane.setPrefColumns(2);
            tilePane.setHgap(12);
            tilePane.setVgap(12);
            tilePane.getChildren().addAll(dishList.stream().map(this::createDishCard).toList());

            VBox section = new VBox(8, header, tilePane);
            section.getStyleClass().add("dish-category-section");
            sections.add(section);
        });
        categoryContainer.getChildren().setAll(sections);
        boolean empty = dishes.isEmpty();
        emptyStateLabel.setVisible(empty);
        categoryContainer.setVisible(!empty);
        updateDeleteModeButton();
    }

    @FXML
    private void toggleDeleteMode() {
        deleteModeEnabled = !deleteModeEnabled;
        updateDeleteModeButton();
        refreshDishes();
    }

    private void updateDeleteModeButton() {
        if (deleteModeButton == null) {
            return;
        }
        if (deleteModeEnabled) {
            deleteModeButton.setText("Terminer la suppression");
            if (!deleteModeButton.getStyleClass().contains("danger-action")) {
                deleteModeButton.getStyleClass().add("danger-action");
            }
        } else {
            deleteModeButton.setText("Supprimer");
            deleteModeButton.getStyleClass().remove("danger-action");
        }
    }

    private Map<DishCategory, List<DishResponseDTO>> groupByCategory(List<DishResponseDTO> dishes) {
        Map<DishCategory, List<DishResponseDTO>> grouped = new EnumMap<>(DishCategory.class);
        for (DishCategory category : DishCategory.values()) {
            grouped.put(category, new ArrayList<>());
        }
        for (DishResponseDTO dish : dishes) {
            grouped.computeIfAbsent(dish.getCategory(), key -> new ArrayList<>()).add(dish);
        }
        return grouped;
    }

    private Node createDishCard(DishResponseDTO dish) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dish-card");
        if (deleteModeEnabled) {
            card.getStyleClass().add("dish-card-delete-mode");
        }
        card.setCursor(deleteModeEnabled ? Cursor.HAND : Cursor.DEFAULT);
        card.setOnMouseClicked(event -> handleDishCardClick(dish));

        ImageView imageView = new ImageView(resolveImageFor(dish));
        imageView.setFitWidth(180);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        StackPane imageWrapper = new StackPane(imageView);
        imageWrapper.getStyleClass().add("dish-image-wrapper");

        Label nameLabel = new Label(dish.getName());
        nameLabel.getStyleClass().add("dish-name");

        Label priceLabel = new Label(String.format(Locale.getDefault(), "%.2f €", dish.getPrice()));
        priceLabel.getStyleClass().add("dish-price");

        Label categoryLabel = new Label(dish.getCategory().name());
        categoryLabel.getStyleClass().add("dish-category");

        card.getChildren().addAll(imageWrapper, nameLabel, priceLabel, categoryLabel);
        return card;
    }

    private void handleDishCardClick(DishResponseDTO dish) {
        if (!deleteModeEnabled || dishController == null || dish == null || dish.getId() == null) {
            return;
        }
        if (!requestDeletePassword(dish.getName())) {
            return;
        }
        try {
            dishController.deleteDish(dish.getId());
            refreshDishes();
        } catch (RuntimeException exception) {
            showAlert(Alert.AlertType.ERROR, "Impossible de supprimer le plat", exception.getMessage());
        }
    }

    private boolean requestDeletePassword(String dishName) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Supprimer un plat");
        dialog.setHeaderText("Entrez le mot de passe pour supprimer \"" + dishName + "\"");
        dialog.setContentText("Mot de passe :");
        Optional<String> password = dialog.showAndWait();
        if (password.isEmpty()) {
            return false;
        }
        if (!DELETE_PASSWORD.equals(password.get().trim())) {
            showAlert(Alert.AlertType.ERROR, "Mot de passe incorrect", "Le mot de passe saisi est incorrect.");
            return false;
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message != null ? message : "");
        alert.showAndWait();
    }

    /**
     * Looks for an image located under src/main/resources/interface/images using a slugified dish name.
     * Example: a dish named "Fish Soup" maps to interface/images/fish-soup.png. Drop PNG/JPG assets there
     * to replace the generated placeholder.
     */
    private Image resolveImageFor(DishResponseDTO dish) {
        String resourceName = slugify(dish.getName());
        String resourcePath = "/interface/images/" + resourceName + ".png";
        InputStream stream = getClass().getResourceAsStream(resourcePath);
        if (stream != null) {
            return new Image(stream, 360, 240, true, true);
        }
        InputStream fallbackStream = getClass().getResourceAsStream("/interface/images/placeholder-dish.png");
        if (fallbackStream != null) {
            return new Image(fallbackStream, 360, 240, true, true);
        }
        return generateSolidImage(dish);
    }

    private String slugify(String input) {
        return input.toLowerCase(Locale.getDefault())
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private Image generateSolidImage(DishResponseDTO dish) {
        int width = 360;
        int height = 240;
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        double hueSeed = dish.getId() != null ? dish.getId() * 37 : dish.getName().hashCode();
        double hue = Math.abs(hueSeed) % 360;
        Color color = Color.hsb(hue, 0.35, 0.95);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, color);
            }
        }
        return image;
    }
}
