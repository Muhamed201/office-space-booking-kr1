package ru.mirea.officebooking.fx.controller;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Window;
import ru.mirea.officebooking.fx.AppContext;
import ru.mirea.officebooking.fx.util.Alerts;
import ru.mirea.officebooking.fx.util.AsyncRunner;
import ru.mirea.officebooking.fx.util.Dialogs;
import ru.mirea.officebooking.fx.util.ErrorMessages;
import ru.mirea.officebooking.fx.util.FxActions;
import ru.mirea.officebooking.fx.util.FxmlSupport;
import ru.mirea.officebooking.fx.util.Labels;
import ru.mirea.officebooking.fx.util.Option;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceStatus;
import ru.mirea.officebooking.model.WorkspaceType;

public class WorkspacesController {

    private final AppContext context;
    private final ObservableList<Workspace> items = FXCollections.observableArrayList();

    @FXML private TableView<Workspace> workspaceTable;
    @FXML private TableColumn<Workspace, String> codeColumn;
    @FXML private TableColumn<Workspace, String> floorColumn;
    @FXML private TableColumn<Workspace, String> typeColumn;
    @FXML private TableColumn<Workspace, String> statusColumn;
    @FXML private TableColumn<Workspace, String> monitorColumn;
    @FXML private TableColumn<Workspace, String> capacityColumn;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button statusButton;
    @FXML private Button deleteButton;
    @FXML private Label statusLabel;

    public WorkspacesController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        workspaceTable.setItems(items);
        workspaceTable.setPlaceholder(new Label("Рабочие места не найдены"));
        bind(codeColumn, Workspace::getCode);
        bind(floorColumn, w -> String.valueOf(w.getFloor()));
        bind(typeColumn, w -> Labels.of(w.type()));
        bind(statusColumn, w -> Labels.of(w.getStatus()));
        bind(monitorColumn, w -> w.isHasMonitor() ? "Есть" : "Нет");
        bind(capacityColumn, w -> w.type() == WorkspaceType.MEETING_ROOM ? String.valueOf(w.getCapacity()) : "");
        workspaceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, row) -> updateButtons(row));
        updateButtons(null);
        reload();
    }

    @FXML
    private void onRefresh() {
        reload();
    }

    @FXML
    private void onAdd() {
        if (Dialogs.showForm(owner(), "Новое рабочее место", "workspace-dialog.fxml",
                new WorkspaceDialogController(context, null))) {
            reload();
        }
    }

    @FXML
    private void onEdit() {
        Workspace selected = workspaceTable.getSelectionModel().getSelectedItem();
        if (selected != null && Dialogs.showForm(owner(), "Редактирование рабочего места", "workspace-dialog.fxml",
                new WorkspaceDialogController(context, selected))) {
            reload();
        }
    }

    @FXML
    private void onChangeStatus() {
        Workspace selected = workspaceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        List<Option<WorkspaceStatus>> options = Arrays.stream(WorkspaceStatus.values())
                .map(status -> new Option<>(Labels.of(status), status))
                .toList();
        ChoiceDialog<Option<WorkspaceStatus>> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.getDialogPane().getButtonTypes().setAll(Alerts.OK, Alerts.CANCEL);
        dialog.setTitle("Статус рабочего места");
        dialog.setHeaderText("Новый статус для " + selected.getCode());
        dialog.setContentText("Статус:");
        dialog.getDialogPane().getStylesheets().add(FxmlSupport.stylesheet());
        if (owner() != null) {
            dialog.initOwner(owner());
        }
        dialog.showAndWait().ifPresent(option ->
                runAction(() -> context.workspaces().changeStatusAs(context.session().userId(), selected.getId(),
                        option.value())));
    }

    @FXML
    private void onDelete() {
        Workspace selected = workspaceTable.getSelectionModel().getSelectedItem();
        if (selected != null && Alerts.confirm(owner(), "Удалить рабочее место?", Labels.of(selected))) {
            runAction(() -> context.workspaces().deleteAs(context.session().userId(), selected.getId()));
        }
    }

    private void runAction(Runnable action) {
        FxActions.run(owner(), action, this::reload);
    }

    private void reload() {
        Workspace selected = workspaceTable.getSelectionModel().getSelectedItem();
        Long selectedId = selected == null ? null : selected.getId();
        AsyncRunner.run(
                () -> context.workspaces().findAll(),
                workspaces -> {
                    items.setAll(workspaces);
                    statusLabel.setText("Рабочих мест: " + workspaces.size());
                    if (selectedId != null) {
                        items.stream().filter(w -> w.getId().equals(selectedId)).findFirst()
                                .ifPresent(w -> workspaceTable.getSelectionModel().select(w));
                    }
                },
                error -> statusLabel.setText(ErrorMessages.of(error)));
    }

    private void updateButtons(Workspace row) {
        boolean manager = context.session().isManager();
        boolean none = row == null;
        addButton.setDisable(!manager);
        editButton.setDisable(none || !manager);
        statusButton.setDisable(none || !manager);
        deleteButton.setDisable(none || !manager);
    }

    private void bind(TableColumn<Workspace, String> column, Function<Workspace, String> getter) {
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
    }

    private Window owner() {
        return workspaceTable.getScene() == null ? null : workspaceTable.getScene().getWindow();
    }
}
