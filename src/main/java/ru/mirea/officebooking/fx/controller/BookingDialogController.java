package ru.mirea.officebooking.fx.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ru.mirea.officebooking.dto.BookingRow;
import ru.mirea.officebooking.fx.AppContext;
import ru.mirea.officebooking.fx.util.AsyncRunner;
import ru.mirea.officebooking.fx.util.ErrorMessages;
import ru.mirea.officebooking.fx.util.FormController;
import ru.mirea.officebooking.fx.util.InputRules;
import ru.mirea.officebooking.fx.util.Labels;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.service.BookingService;

public class BookingDialogController implements FormController {

    private static final int MAX_ATTENDEES_DIGITS = 4;

    private record Choices(List<Employee> employees, List<Workspace> workspaces) {
    }

    private final AppContext context;
    private final BookingRow editing;

    @FXML private ComboBox<Employee> employeeCombo;
    @FXML private ComboBox<Workspace> workspaceCombo;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Integer> startHourCombo;
    @FXML private ComboBox<Integer> endHourCombo;
    @FXML private TextField attendeesField;
    @FXML private TextArea purposeArea;
    @FXML private Label errorLabel;

    public BookingDialogController(AppContext context, BookingRow editing) {
        this.context = context;
        this.editing = editing;
    }

    @FXML
    private void initialize() {
        employeeCombo.setConverter(Labels.converter(Labels::of));
        workspaceCombo.setConverter(Labels.converter(Labels::of));
        StringConverter<Integer> hourConverter = Labels.converter(hour -> "%02d:00".formatted(hour));
        startHourCombo.setConverter(hourConverter);
        endHourCombo.setConverter(hourConverter);
        InputRules.dateOnly(datePicker, LocalDate::now,
                () -> LocalDate.now().plusDays(BookingService.BOOKING_HORIZON_DAYS));
        InputRules.digits(attendeesField, MAX_ATTENDEES_DIGITS);
        InputRules.maxLength(purposeArea, Booking.MAX_PURPOSE_LENGTH);

        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> refreshStartHours());
        startHourCombo.valueProperty().addListener((obs, oldHour, newHour) -> refreshEndHours());
        workspaceCombo.valueProperty().addListener((obs, oldPlace, newPlace) -> onWorkspaceChanged());
        employeeCombo.setDisable(true);
        workspaceCombo.setDisable(true);
        onWorkspaceChanged();

        if (editing == null) {
            datePicker.setValue(LocalDate.now());
            loadForCreate();
        } else {
            datePicker.setValue(editing.startTime().toLocalDate());
            purposeArea.setText(editing.purpose() == null ? "" : editing.purpose());
            loadForEdit();
        }
    }

    private void loadForCreate() {
        boolean manager = context.session().isManager();
        Employee currentUser = context.session().user();
        AsyncRunner.run(
                () -> new Choices(manager ? context.employees().findAvailableForBooking() : List.of(currentUser),
                        context.workspaces().findAvailableForBooking()),
                choices -> {
                    employeeCombo.getItems().setAll(choices.employees());
                    workspaceCombo.getItems().setAll(choices.workspaces());
                    choices.employees().stream()
                            .filter(e -> e.getId().equals(currentUser.getId()))
                            .findFirst()
                            .ifPresent(employeeCombo::setValue);
                    employeeCombo.setDisable(!manager);
                    workspaceCombo.setDisable(false);
                },
                error -> showError(ErrorMessages.of(error)));
    }

    private void loadForEdit() {
        AsyncRunner.run(
                () -> new Choices(List.of(context.employees().findById(editing.employeeId())),
                        List.of(context.workspaces().findById(editing.workspaceId()))),
                choices -> {
                    employeeCombo.getItems().setAll(choices.employees());
                    workspaceCombo.getItems().setAll(choices.workspaces());
                    employeeCombo.getSelectionModel().selectFirst();
                    workspaceCombo.getSelectionModel().selectFirst();
                    startHourCombo.setValue(editing.startTime().getHour());
                    endHourCombo.setValue(editing.endTime().getHour());
                    attendeesField.setText(editing.attendees() == null ? "" : String.valueOf(editing.attendees()));
                },
                error -> showError(ErrorMessages.of(error)));
    }

    private void refreshStartHours() {
        Integer current = startHourCombo.getValue();
        LocalDate date = datePicker.getValue();
        int first = BookingService.WORKDAY_START.getHour();
        if (LocalDate.now().equals(date)) {
            first = Math.max(first, LocalTime.now().getHour() + 1);
        }
        List<Integer> hours = IntStream.range(first, BookingService.WORKDAY_END.getHour()).boxed().toList();
        startHourCombo.getItems().setAll(hours);
        startHourCombo.setValue(hours.contains(current) ? current : hours.isEmpty() ? null : hours.get(0));
        refreshEndHours();
    }

    private void refreshEndHours() {
        Integer start = startHourCombo.getValue();
        Integer current = endHourCombo.getValue();
        if (start == null) {
            endHourCombo.getItems().clear();
            endHourCombo.setValue(null);
            return;
        }
        int last = BookingService.WORKDAY_END.getHour();
        Workspace workspace = workspaceCombo.getValue();
        if (workspace != null) {
            last = Math.min(last, start + workspace.maxBookingHours());
        }
        List<Integer> hours = IntStream.rangeClosed(start + 1, last).boxed().toList();
        endHourCombo.getItems().setAll(hours);
        endHourCombo.setValue(hours.contains(current) ? current : hours.get(0));
    }

    private void onWorkspaceChanged() {
        Workspace workspace = workspaceCombo.getValue();
        boolean meetingRoom = workspace != null && workspace.type() == WorkspaceType.MEETING_ROOM;
        attendeesField.setDisable(!meetingRoom);
        if (!meetingRoom) {
            attendeesField.clear();
        }
        attendeesField.setPromptText(meetingRoom
                ? "от 1 до %d".formatted(workspace.getCapacity())
                : "только для переговорной");
        refreshEndHours();
    }

    @Override
    public Runnable prepareSave() {
        showError("");
        InputRules.clearInvalid(employeeCombo, workspaceCombo, datePicker, startHourCombo, endHourCombo,
                attendeesField, purposeArea);
        Employee employee = employeeCombo.getValue();
        Workspace workspace = workspaceCombo.getValue();
        LocalDate date = datePicker.getValue();
        Integer startHour = startHourCombo.getValue();
        Integer endHour = endHourCombo.getValue();
        if (employee == null) {
            return fail(employeeCombo, "Выберите сотрудника");
        }
        if (workspace == null) {
            return fail(workspaceCombo, "Выберите рабочее место");
        }
        if (date == null) {
            return fail(datePicker, "Выберите дату брони в календаре");
        }
        if (startHour == null) {
            return fail(datePicker, "На выбранную дату свободного времени уже не осталось, выберите другую дату");
        }
        if (endHour == null || endHour <= startHour) {
            return fail(endHourCombo, "Время окончания должно быть позже времени начала");
        }
        Integer attendees = null;
        if (workspace.type() == WorkspaceType.MEETING_ROOM) {
            String text = attendeesField.getText() == null ? "" : attendeesField.getText().trim();
            if (text.isEmpty()) {
                return fail(attendeesField,
                        "Укажите число участников: от 1 до %d".formatted(workspace.getCapacity()));
            }
            attendees = Integer.parseInt(text);
            if (attendees < 1 || attendees > workspace.getCapacity()) {
                return fail(attendeesField, "Число участников должно быть от 1 до %d (вместимость %s)"
                        .formatted(workspace.getCapacity(), workspace.getCode()));
            }
        }
        return buildAction(employee.getId(), workspace.getId(), date.atTime(startHour, 0), date.atTime(endHour, 0),
                attendees, purposeArea.getText());
    }

    @Override
    public void showError(String message) {
        errorLabel.setText(message);
    }

    private Runnable buildAction(long employeeId, long workspaceId, LocalDateTime start, LocalDateTime end,
                                 Integer attendees, String purpose) {
        long actorId = context.session().userId();
        if (editing == null) {
            return () -> context.bookings().createAs(actorId, employeeId, workspaceId, start, end, attendees,
                    purpose);
        }
        long bookingId = editing.id();
        return () -> context.bookings().updateTimeAs(actorId, bookingId, start, end, attendees, purpose);
    }

    private Runnable fail(Control field, String message) {
        InputRules.markInvalid(field);
        showError(message);
        return null;
    }
}
