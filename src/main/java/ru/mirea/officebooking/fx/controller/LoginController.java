package ru.mirea.officebooking.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.officebooking.exception.ValidationException;
import ru.mirea.officebooking.fx.AppContext;
import ru.mirea.officebooking.fx.util.FormController;
import ru.mirea.officebooking.fx.util.InputRules;
import ru.mirea.officebooking.model.Employee;

public class LoginController implements FormController {

    private final AppContext context;

    @FXML private TextField emailField;
    @FXML private Label errorLabel;

    public LoginController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        InputRules.email(emailField, Employee.MAX_EMAIL_LENGTH);
    }

    @Override
    public Runnable prepareSave() {
        showError("");
        InputRules.clearInvalid(emailField);
        String email = emailField.getText();
        if (email == null || email.isBlank()) {
            InputRules.markInvalid(emailField);
            showError("Введите рабочий email");
            return null;
        }
        try {
            Employee.normalizeEmail(email);
        } catch (ValidationException e) {
            InputRules.markInvalid(emailField);
            showError(e.getMessage());
            return null;
        }
        return () -> context.session().login(context.employees().login(email));
    }

    @Override
    public void showError(String message) {
        errorLabel.setText(message);
    }
}
