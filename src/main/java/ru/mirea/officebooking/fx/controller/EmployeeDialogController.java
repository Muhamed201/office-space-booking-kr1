package ru.mirea.officebooking.fx.controller;

import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.officebooking.exception.ValidationException;
import ru.mirea.officebooking.fx.AppContext;
import ru.mirea.officebooking.fx.util.FormController;
import ru.mirea.officebooking.fx.util.InputRules;
import ru.mirea.officebooking.fx.util.Labels;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.EmployeeRole;

public class EmployeeDialogController implements FormController {

    private final AppContext context;
    private final Employee editing;

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField departmentField;
    @FXML private TextField positionField;
    @FXML private DatePicker hireDatePicker;
    @FXML private ComboBox<EmployeeRole> roleCombo;
    @FXML private Label errorLabel;

    public EmployeeDialogController(AppContext context, Employee editing) {
        this.context = context;
        this.editing = editing;
    }

    @FXML
    private void initialize() {
        roleCombo.setConverter(Labels.converter(Labels::of));
        roleCombo.getItems().setAll(EmployeeRole.values());
        InputRules.personName(fullNameField, Employee.MAX_FULL_NAME_LENGTH);
        InputRules.email(emailField, Employee.MAX_EMAIL_LENGTH);
        InputRules.maxLength(departmentField, Employee.MAX_DEPARTMENT_LENGTH);
        InputRules.maxLength(positionField, Employee.MAX_POSITION_LENGTH);
        InputRules.dateOnly(hireDatePicker, () -> LocalDate.now().minusYears(Employee.MAX_YEARS_SINCE_HIRE),
                LocalDate::now);
        if (editing == null) {
            roleCombo.setValue(EmployeeRole.EMPLOYEE);
            hireDatePicker.setValue(LocalDate.now());
        } else {
            fullNameField.setText(editing.getFullName());
            emailField.setText(editing.getEmail());
            departmentField.setText(editing.getDepartment());
            positionField.setText(editing.getPosition() == null ? "" : editing.getPosition());
            hireDatePicker.setValue(editing.getHireDate());
            roleCombo.setValue(editing.getRole());
        }
    }

    @Override
    public Runnable prepareSave() {
        showError("");
        InputRules.clearInvalid(fullNameField, emailField, departmentField, positionField, hireDatePicker, roleCombo);
        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String department = departmentField.getText();
        String position = positionField.getText();
        LocalDate hireDate = hireDatePicker.getValue();
        EmployeeRole role = roleCombo.getValue();
        if (!check(fullNameField, () -> Employee.normalizeFullName(fullName))
                || !check(emailField, () -> Employee.normalizeEmail(email))
                || !check(departmentField, () -> Employee.normalizeDepartment(department))
                || !check(positionField, () -> Employee.normalizePosition(position))) {
            return null;
        }
        if (hireDate == null) {
            return fail(hireDatePicker, "Выберите дату приёма на работу в календаре");
        }
        if (!check(hireDatePicker, () -> Employee.validateHireDate(hireDate))) {
            return null;
        }
        if (role == null) {
            return fail(roleCombo, "Выберите роль");
        }
        long actorId = context.session().userId();
        if (editing == null) {
            return () -> context.employees().createAs(actorId, fullName, email, department, position, hireDate,
                    role);
        }
        long id = editing.getId();
        return () -> context.employees().updateAs(actorId, id, fullName, email, department, position, hireDate,
                role);
    }

    @Override
    public void showError(String message) {
        errorLabel.setText(message);
    }

    private boolean check(Control field, Runnable rule) {
        try {
            rule.run();
            return true;
        } catch (ValidationException e) {
            fail(field, e.getMessage());
            return false;
        }
    }

    private Runnable fail(Control field, String message) {
        InputRules.markInvalid(field);
        showError(message);
        return null;
    }
}
