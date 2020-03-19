package helpers;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

public class DialogsHelper {

    public Optional<String> showInputDialog(String header, String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(header);
        dialog.setHeaderText(null);
        dialog.setContentText(message);

        return dialog.showAndWait();
    }

    public void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(header);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    public boolean getFeedbackFromConfirmationAlert(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(header);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
