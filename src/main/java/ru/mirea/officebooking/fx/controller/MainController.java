package ru.mirea.officebooking.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import ru.mirea.officebooking.fx.AppContext;
import ru.mirea.officebooking.fx.util.Labels;
import ru.mirea.officebooking.model.Employee;

public class MainController {

    private final AppContext context;

    @FXML private Label userLabel;

    public MainController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        Employee user = context.session().user();
        userLabel.setText("Вы вошли как: %s · %s".formatted(user.getFullName(), Labels.of(user.getRole())));
    }

    @FXML
    private void onLogout() {
        context.session().logout();
    }
}
