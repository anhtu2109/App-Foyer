import backend.dto.DishResponseDTO;
import backend.entity.DishCategory;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public final class EditDishDialog {
    private EditDishDialog() {
    }

    public static Optional<DishResponseDTO> show(DishResponseDTO existingDish) {
        if (existingDish == null) {
            return Optional.empty();
        }
        Dialog<DishResponseDTO> dialog = new Dialog<>();
        dialog.setTitle("Modifier un plat");
        dialog.setHeaderText("Mettez à jour les informations pour \"" + existingDish.getName() + "\"");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField(existingDish.getName());
        TextField priceField = new TextField(String.valueOf(existingDish.getPrice()));
        ComboBox<DishCategory> categoryComboBox = new ComboBox<>(FXCollections.observableArrayList(DishCategory.values()));
        categoryComboBox.setValue(existingDish.getCategory());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Nom"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Prix"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Catégorie"), 0, 2);
        grid.add(categoryComboBox, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        BooleanBinding isFormValid = Bindings.createBooleanBinding(
                () -> isNameValid(nameField.getText())
                        && isPriceValid(priceField.getText())
                        && categoryComboBox.getValue() != null,
                nameField.textProperty(),
                priceField.textProperty(),
                categoryComboBox.valueProperty());
        saveButton.disableProperty().bind(isFormValid.not());

        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                DishResponseDTO dto = new DishResponseDTO();
                dto.setId(existingDish.getId());
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
