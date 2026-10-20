package ru.mirea.officebooking.fx.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public final class Alerts {

    public static final ButtonType OK = new ButtonType("ОК", ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType CANCEL = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

    private Alerts() {
    }

    public static void error(Window owner, String header, String message) {
        show(Alert.AlertType.ERROR, owner, "Ошибка", header, message);
    }

    public static void info(Window owner, String header, String message) {
        show(Alert.AlertType.INFORMATION, owner, "Готово", header, message);
    }

    public static boolean confirm(Window owner, String header, String message) {
        Alert alert = create(Alert.AlertType.CONFIRMATION, owner, "Подтверждение", header, message);
        alert.getButtonTypes().setAll(OK, CANCEL);
        return alert.showAndWait().filter(OK::equals).isPresent();
    }

    private static void show(Alert.AlertType type, Window owner, String title, String header, String message) {
        create(type, owner, title, header, message).showAndWait();
    }

    private static Alert create(Alert.AlertType type, Window owner, String title, String header, String message) {
        Alert alert = new Alert(type);
        alert.getButtonTypes().setAll(OK);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(FxmlSupport.stylesheet());
        return alert;
    }
}
