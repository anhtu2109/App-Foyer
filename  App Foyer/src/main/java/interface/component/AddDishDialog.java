import backend.dto.DishRequestDTO;
import backend.entity.DishCategory;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public final class AddDishDialog {
    private AddDishDialog() {
    }

    public static Optional<DishRequestDTO> show() {
        Dialog<DishRequestDTO> dialog = new Dialog<>();
        dialog.setTitle("Add Dish");
        dialog.setHeaderText("Nhập thông tin món mới");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Dish name");

        TextField priceField = new TextField();
        priceField.setPromptText("Price");

        ComboBox<DishCategory> categoryComboBox = new ComboBox<>(FXCollections.observableArrayList(DishCategory.values()));
        categoryComboBox.setPromptText("Category");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Name"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Price"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Category"), 0, 2);
        grid.add(categoryComboBox, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        BooleanBinding isFormValid = Bindings.createBooleanBinding(
                () -> isNameValid(nameField.getText())
                        && isPriceValid(priceField.getText())
                        && categoryComboBox.getValue() != null,
                nameField.textProperty(),
                priceField.textProperty(),
                categoryComboBox.valueProperty());
        addButton.disableProperty().bind(isFormValid.not());

        dialog.setResultConverter(button -> {
            if (button == addButtonType) {
                DishRequestDTO dto = new DishRequestDTO();
                dto.setName(nameField.getText().trim());
                dto.setPrice(Double.parseDouble(priceField.getText().trim()));
                dto.setCategory(categoryComboBox.getValue());
                return dto;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static boolean isNameValid(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean isPriceValid(String value) {
        if (value == null) {
            return false;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            return parsed >= 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
