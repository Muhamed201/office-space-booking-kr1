package ru.mirea.officebooking.fx.util;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

public final class Dialogs {

    private Dialogs() {
    }

    public static <C extends FormController> boolean showForm(Window owner, String title, String fxmlName, C controller) {
        return showForm(owner, title, fxmlName, controller, "Сохранить");
    }

    public static <C extends FormController> boolean showForm(Window owner, String title, String fxmlName, C controller,
                                                              String submitText) {
        Parent content;
        try {
            content = FxmlSupport.load(fxmlName, FxmlSupport.factoryFor(controller));
        } catch (IOException e) {
            Alerts.error(owner, "Не удалось открыть форму", ErrorMessages.of(e));
            return false;
        }
        ButtonType saveType = new ButtonType(submitText, ButtonBar.ButtonData.OK_DONE);
        DialogPane pane = new DialogPane();
        pane.setContent(content);
        pane.getButtonTypes().addAll(saveType, Alerts.CANCEL);
        pane.getStylesheets().add(FxmlSupport.stylesheet());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(pane);
        dialog.setTitle(title);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        Button saveButton = (Button) pane.lookupButton(saveType);
        saveButton.getStyleClass().add("button-primary");
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            Runnable action = controller.prepareSave();
            if (action == null) {
                return;
            }
            saveButton.setDisable(true);
            AsyncRunner.run(
                    () -> {
                        action.run();
                        return Boolean.TRUE;
                    },
                    done -> {
                        dialog.setResult(saveType);
                        dialog.close();
                    },
                    error -> {
                        controller.showError(ErrorMessages.of(error));
                        saveButton.setDisable(false);
                    });
        });
        return dialog.showAndWait().filter(saveType::equals).isPresent();
    }
}
