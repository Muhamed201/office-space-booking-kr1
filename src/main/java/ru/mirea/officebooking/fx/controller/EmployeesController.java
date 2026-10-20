package ru.mirea.officebooking.fx.controller;

import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import ru.mirea.officebooking.fx.AppContext;
import ru.mirea.officebooking.fx.util.Alerts;
import ru.mirea.officebooking.fx.util.AsyncRunner;
import ru.mirea.officebooking.fx.util.Dialogs;
import ru.mirea.officebooking.fx.util.ErrorMessages;
import ru.mirea.officebooking.fx.util.FxActions;
import ru.mirea.officebooking.fx.util.InputRules;
import ru.mirea.officebooking.fx.util.Labels;
import ru.mirea.officebooking.model.Employee;

public class EmployeesController {

    private static final int SEARCH_MAX_LENGTH = 100;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final AppContext context;
    private final ObservableList<Employee> items = FXCollections.observableArrayList();
    private final FilteredList<Employee> filtered = new FilteredList<>(items, employee -> true);

    @FXML private TextField searchField;
    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, String> nameColumn;
    @FXML private TableColumn<Employee, String> emailColumn;
    @FXML private TableColumn<Employee, String> departmentColumn;
    @FXML private TableColumn<Employee, String> positionColumn;
    @FXML private TableColumn<Employee, String> hireDateColumn;
    @FXML private TableColumn<Employee, String> roleColumn;
    @FXML private TableColumn<Employee, String> blockedColumn;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button blockButton;
    @FXML private Label statusLabel;

    public EmployeesController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        InputRules.maxLength(searchField, SEARCH_MAX_LENGTH);
        employeeTable.setItems(filtered);
        employeeTable.setPlaceholder(new Label("Сотрудники не найдены"));
        bind(nameColumn, Employee::getFullName);
        bind(emailColumn, Employee::getEmail);
        bind(departmentColumn, Employee::getDepartment);
        bind(positionColumn, e -> e.getPosition() == null ? "" : e.getPosition());
        bind(hireDateColumn, e -> e.getHireDate().format(DATE));
        bind(roleColumn, e -> Labels.of(e.getRole()));
        bind(blockedColumn, e -> e.isBlocked() ? "Да" : "Нет");

        searchField.textProperty().addListener((obs, oldValue, text) -> {
            String query = text == null ? "" : text.trim().toLowerCase();
            filtered.setPredicate(e -> query.isEmpty()
                    || e.getFullName().toLowerCase().contains(query)
                    || e.getEmail().toLowerCase().contains(query)
                    || e.getDepartment().toLowerCase().contains(query));
            statusLabel.setText("Показано сотрудников: " + filtered.size());
        });
        employeeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, row) -> updateButtons(row));
        updateButtons(null);
        reload();
    }

    @FXML
    private void onRefresh() {
        reload();
    }

    @FXML
    private void onAdd() {
        if (Dialogs.showForm(owner(), "Новый сотрудник", "employee-dialog.fxml",
                new EmployeeDialogController(context, null))) {
            reload();
        }
    }

    @FXML
    private void onEdit() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected != null && Dialogs.showForm(owner(), "Редактирование сотрудника", "employee-dialog.fxml",
                new EmployeeDialogController(context, selected))) {
            reload();
        }
    }

    @FXML
    private void onDelete() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (Alerts.confirm(owner(), "Удалить сотрудника?", selected.getFullName() + " (" + selected.getEmail() + ")")) {
            runAction(() -> context.employees().deleteAs(context.session().userId(), selected.getId()));
        }
    }

    @FXML
    private void onToggleBlocked() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            runAction(() -> context.employees().setBlockedAs(context.session().userId(), selected.getId(),
                    !selected.isBlocked()));
        }
    }

    private void runAction(Runnable action) {
        FxActions.run(owner(), action, this::reload);
    }

    private void reload() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        Long selectedId = selected == null ? null : selected.getId();
        AsyncRunner.run(
                () -> context.employees().findAll(),
                employees -> {
                    items.setAll(employees);
                    statusLabel.setText("Показано сотрудников: " + filtered.size());
                    if (selectedId != null) {
                        items.stream().filter(e -> e.getId().equals(selectedId)).findFirst()
                                .ifPresent(e -> employeeTable.getSelectionModel().select(e));
                    }
                },
                error -> statusLabel.setText(ErrorMessages.of(error)));
    }

    private void updateButtons(Employee row) {
        boolean manager = context.session().isManager();
        boolean none = row == null;
        boolean self = !none && context.session().owns(row.getId());
        addButton.setDisable(!manager);
        editButton.setDisable(none || !manager);
        deleteButton.setDisable(none || !manager || self);
        blockButton.setDisable(none || !manager || self);
        blockButton.setText(!none && row.isBlocked() ? "Разблокировать" : "Заблокировать");
    }

    private void bind(TableColumn<Employee, String> column, Function<Employee, String> getter) {
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
    }

    private Window owner() {
        return employeeTable.getScene() == null ? null : employeeTable.getScene().getWindow();
    }
}
