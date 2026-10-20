package ru.mirea.officebooking.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.officebooking.exception.ValidationException;
import ru.mirea.officebooking.fx.AppContext;
import ru.mirea.officebooking.fx.util.FormController;
import ru.mirea.officebooking.fx.util.InputRules;
import ru.mirea.officebooking.fx.util.Labels;
import ru.mirea.officebooking.model.MeetingRoom;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceType;

public class WorkspaceDialogController implements FormController {

    private static final int FLOOR_DIGITS = 2;
    private static final int CAPACITY_DIGITS = 3;

    private final AppContext context;
    private final Workspace editing;

    @FXML private TextField codeField;
    @FXML private TextField floorField;
    @FXML private ComboBox<WorkspaceType> typeCombo;
    @FXML private CheckBox monitorCheck;
    @FXML private TextField capacityField;
    @FXML private Label errorLabel;

    public WorkspaceDialogController(AppContext context, Workspace editing) {
        this.context = context;
        this.editing = editing;
    }

    @FXML
    private void initialize() {
        typeCombo.setConverter(Labels.converter(Labels::of));
        typeCombo.getItems().setAll(WorkspaceType.values());
        InputRules.workspaceCode(codeField, Workspace.MAX_CODE_LENGTH);
        InputRules.signedDigits(floorField, FLOOR_DIGITS);
        InputRules.digits(capacityField, CAPACITY_DIGITS);
        typeCombo.valueProperty().addListener((obs, oldType, newType) -> {
            boolean meetingRoom = newType == WorkspaceType.MEETING_ROOM;
            capacityField.setDisable(editing != null || !meetingRoom);
            if (editing == null && !meetingRoom) {
                capacityField.clear();
            }
        });
        if (editing == null) {
            typeCombo.setValue(WorkspaceType.OPEN_DESK);
        } else {
            codeField.setText(editing.getCode());
            floorField.setText(String.valueOf(editing.getFloor()));
            typeCombo.setValue(editing.type());
            monitorCheck.setSelected(editing.isHasMonitor());
            capacityField.setText(editing.type() == WorkspaceType.MEETING_ROOM
                    ? String.valueOf(editing.getCapacity()) : "");
            typeCombo.setDisable(true);
            capacityField.setDisable(true);
        }
    }

    @Override
    public Runnable prepareSave() {
        showError("");
        InputRules.clearInvalid(typeCombo, codeField, floorField, capacityField);
        WorkspaceType type = typeCombo.getValue();
        String code = codeField.getText();
        if (type == null) {
            return fail(typeCombo, "Выберите тип места");
        }
        if (!check(codeField, () -> Workspace.normalizeCode(code))) {
            return null;
        }
        Integer floor = parseInt(floorField.getText());
        if (floor == null) {
            return fail(floorField, "Укажите этаж целым числом от %d до %d"
                    .formatted(Workspace.MIN_FLOOR, Workspace.MAX_FLOOR));
        }
        if (!check(floorField, () -> Workspace.validateFloor(floor))) {
            return null;
        }
        Integer capacity = null;
        if (editing == null && type == WorkspaceType.MEETING_ROOM) {
            capacity = parseInt(capacityField.getText());
            if (capacity == null) {
                return fail(capacityField, "Укажите вместимость переговорной: от %d до %d"
                        .formatted(MeetingRoom.MIN_CAPACITY, MeetingRoom.MAX_CAPACITY));
            }
            int value = capacity;
            if (!check(capacityField, () -> MeetingRoom.validateCapacity(value))) {
                return null;
            }
        }
        boolean hasMonitor = monitorCheck.isSelected();
        long actorId = context.session().userId();
        if (editing == null) {
            Integer meetingCapacity = capacity;
            return () -> context.workspaces().createAs(actorId, type, code, floor, hasMonitor, meetingCapacity);
        }
        long id = editing.getId();
        return () -> context.workspaces().updateAs(actorId, id, code, floor, hasMonitor);
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

    private static Integer parseInt(String text) {
        if (text == null || text.isBlank() || "-".equals(text.trim())) {
            return null;
        }
        return Integer.parseInt(text.trim());
    }
}
