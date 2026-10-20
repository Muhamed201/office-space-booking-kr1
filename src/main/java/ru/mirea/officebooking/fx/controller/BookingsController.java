package ru.mirea.officebooking.fx.controller;

import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import ru.mirea.officebooking.dto.BookingFilter;
import ru.mirea.officebooking.dto.BookingRow;
import ru.mirea.officebooking.dto.BookingSort;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.fx.AppContext;
import ru.mirea.officebooking.fx.util.Alerts;
import ru.mirea.officebooking.fx.util.AsyncRunner;
import ru.mirea.officebooking.fx.util.Dialogs;
import ru.mirea.officebooking.fx.util.ErrorMessages;
import ru.mirea.officebooking.fx.util.FxActions;
import ru.mirea.officebooking.fx.util.InputRules;
import ru.mirea.officebooking.fx.util.Labels;
import ru.mirea.officebooking.fx.util.Option;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.WorkspaceType;

public class BookingsController {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final long SEARCH_DELAY_MS = 300;

    private final AppContext context;
    private final ObservableList<BookingRow> items = FXCollections.observableArrayList();
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(SEARCH_DELAY_MS));
    private int requestSeq;
    private boolean suspendReload;

    @FXML private TextField searchField;
    @FXML private ComboBox<Option<BookingStatus>> statusCombo;
    @FXML private ComboBox<Option<WorkspaceType>> typeCombo;
    @FXML private ComboBox<Option<Integer>> floorCombo;
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;
    @FXML private ComboBox<BookingSort> sortCombo;
    @FXML private CheckBox ascCheck;

    @FXML private TableView<BookingRow> bookingTable;
    @FXML private TableColumn<BookingRow, String> idColumn;
    @FXML private TableColumn<BookingRow, String> employeeColumn;
    @FXML private TableColumn<BookingRow, String> workspaceColumn;
    @FXML private TableColumn<BookingRow, String> typeColumn;
    @FXML private TableColumn<BookingRow, String> floorColumn;
    @FXML private TableColumn<BookingRow, String> startColumn;
    @FXML private TableColumn<BookingRow, String> endColumn;
    @FXML private TableColumn<BookingRow, String> statusColumn;
    @FXML private TableColumn<BookingRow, String> attendeesColumn;
    @FXML private TableColumn<BookingRow, String> purposeColumn;

    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button cancelBookingButton;
    @FXML private Button completeButton;
    @FXML private Button noShowButton;
    @FXML private Label statusLabel;

    public BookingsController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        InputRules.maxLength(searchField, BookingFilter.MAX_TEXT_LENGTH);
        InputRules.dateRange(dateFromPicker, dateToPicker);
        configureColumns();
        configureFilters();
        configureSelectionButtons();
        searchDelay.setOnFinished(event -> reload());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> searchDelay.playFromStart());
        loadFloors();
        reload();
    }

    @FXML
    private void onRefresh() {
        loadFloors();
        reload();
    }

    @FXML
    private void onReset() {
        suspendReload = true;
        searchField.clear();
        statusCombo.getSelectionModel().selectFirst();
        typeCombo.getSelectionModel().selectFirst();
        floorCombo.getSelectionModel().selectFirst();
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
        sortCombo.setValue(BookingSort.START_TIME);
        ascCheck.setSelected(false);
        suspendReload = false;
        reload();
    }

    @FXML
    private void onAdd() {
        BookingDialogController form = new BookingDialogController(context, null);
        if (Dialogs.showForm(owner(), "Новая бронь", "booking-dialog.fxml", form)) {
            reload();
        }
    }

    @FXML
    private void onEdit() {
        BookingRow selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        BookingDialogController form = new BookingDialogController(context, selected);
        if (Dialogs.showForm(owner(), "Редактирование брони №" + selected.id(), "booking-dialog.fxml", form)) {
            reload();
        }
    }

    @FXML
    private void onDelete() {
        BookingRow selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        String message = "Бронь №%d: %s, %s, %s – %s".formatted(selected.id(), selected.employeeName(),
                selected.workspaceCode(), selected.startTime().format(DATE_TIME), selected.endTime().format(DATE_TIME));
        if (Alerts.confirm(owner(), "Удалить бронь безвозвратно?", message)) {
            runAction(() -> context.bookings().deleteAs(context.session().userId(), selected.id()));
        }
    }

    @FXML
    private void onCancelBooking() {
        changeStatus(BookingStatus.CANCELLED);
    }

    @FXML
    private void onComplete() {
        changeStatus(BookingStatus.COMPLETED);
    }

    @FXML
    private void onNoShow() {
        changeStatus(BookingStatus.NO_SHOW);
    }

    @FXML
    private void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт списка броней");
        chooser.setInitialFileName("bookings.xlsx");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(owner());
        if (file == null) {
            return;
        }
        Path target = withExtension(file, chooser.getSelectedExtensionFilter());
        List<BookingRow> snapshot = List.copyOf(items);
        AsyncRunner.run(
                () -> {
                    context.export().exportRows(snapshot, target);
                    return snapshot.size();
                },
                count -> Alerts.info(owner(), "Экспорт выполнен", "Записей: %d%nФайл: %s".formatted(count, target)),
                error -> Alerts.error(owner(), "Не удалось выполнить экспорт", ErrorMessages.of(error)));
    }

    private void changeStatus(BookingStatus target) {
        BookingRow selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        runAction(() -> context.bookings().changeStatusAs(context.session().userId(), selected.id(), target));
    }

    private void runAction(Runnable action) {
        FxActions.run(owner(), action, this::reload);
    }

    private void reload() {
        if (suspendReload) {
            return;
        }
        BookingFilter filter;
        try {
            filter = new BookingFilter(
                    searchField.getText(),
                    statusCombo.getValue().value(),
                    typeCombo.getValue().value(),
                    floorCombo.getValue() == null ? null : floorCombo.getValue().value(),
                    dateFromPicker.getValue(),
                    dateToPicker.getValue());
        } catch (AppException e) {
            showStatus(e.getMessage(), true);
            return;
        }
        BookingSort sort = sortCombo.getValue();
        boolean ascending = ascCheck.isSelected();
        BookingRow selected = bookingTable.getSelectionModel().getSelectedItem();
        Long selectedId = selected == null ? null : selected.id();
        int seq = ++requestSeq;
        AsyncRunner.run(
                () -> context.bookingQueries().search(filter, sort, ascending),
                rows -> {
                    if (seq != requestSeq) {
                        return;
                    }
                    items.setAll(rows);
                    restoreSelection(selectedId);
                    showStatus("Найдено броней: " + rows.size(), false);
                },
                error -> {
                    if (seq == requestSeq) {
                        showStatus(ErrorMessages.of(error), true);
                    }
                });
    }

    private void loadFloors() {
        AsyncRunner.run(
                () -> context.bookingQueries().availableFloors(),
                floors -> {
                    Integer current = floorCombo.getValue() == null ? null : floorCombo.getValue().value();
                    suspendReload = true;
                    floorCombo.getItems().setAll(Option.withAll("Все этажи", floors, floor -> "Этаж " + floor));
                    floorCombo.getItems().stream()
                            .filter(option -> Objects.equals(option.value(), current))
                            .findFirst()
                            .ifPresent(floorCombo.getSelectionModel()::select);
                    suspendReload = false;
                },
                error -> showStatus(ErrorMessages.of(error), true));
    }

    private void restoreSelection(Long id) {
        if (id == null) {
            return;
        }
        items.stream().filter(row -> row.id() == id).findFirst()
                .ifPresent(row -> bookingTable.getSelectionModel().select(row));
    }

    private void configureColumns() {
        bookingTable.setItems(items);
        bookingTable.setPlaceholder(new Label("Нет броней, подходящих под условия"));
        bind(idColumn, row -> String.valueOf(row.id()));
        bind(employeeColumn, BookingRow::employeeName);
        bind(workspaceColumn, BookingRow::workspaceCode);
        bind(typeColumn, row -> Labels.of(row.workspaceType()));
        bind(floorColumn, row -> String.valueOf(row.floor()));
        bind(startColumn, row -> row.startTime().format(DATE_TIME));
        bind(endColumn, row -> row.endTime().format(DATE_TIME));
        bind(statusColumn, row -> Labels.of(row.status()));
        bind(attendeesColumn, row -> row.attendees() == null ? "" : String.valueOf(row.attendees()));
        bind(purposeColumn, row -> row.purpose() == null ? "" : row.purpose());

        Arrays.asList(idColumn, typeColumn, floorColumn, endColumn, attendeesColumn, purposeColumn)
                .forEach(column -> column.setSortable(false));
        sortable(employeeColumn, BookingSort.EMPLOYEE_NAME);
        sortable(workspaceColumn, BookingSort.WORKSPACE_CODE);
        sortable(startColumn, BookingSort.START_TIME);
        sortable(statusColumn, BookingSort.STATUS);
        bookingTable.setSortPolicy(table -> {
            applyHeaderSort();
            return true;
        });
    }

    private void applyHeaderSort() {
        if (bookingTable.getSortOrder().isEmpty()) {
            return;
        }
        TableColumn<BookingRow, ?> column = bookingTable.getSortOrder().get(0);
        if (!(column.getUserData() instanceof BookingSort sort)) {
            return;
        }
        boolean ascending = column.getSortType() == TableColumn.SortType.ASCENDING;
        if (sort == sortCombo.getValue() && ascending == ascCheck.isSelected()) {
            return;
        }
        suspendReload = true;
        sortCombo.setValue(sort);
        ascCheck.setSelected(ascending);
        suspendReload = false;
        reload();
    }

    private void sortable(TableColumn<BookingRow, String> column, BookingSort sort) {
        column.setUserData(sort);
        column.setSortable(true);
    }

    private void bind(TableColumn<BookingRow, String> column, Function<BookingRow, String> getter) {
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
    }

    private void configureFilters() {
        statusCombo.getItems().setAll(Option.withAll("Все статусы", List.of(BookingStatus.values()), Labels::of));
        typeCombo.getItems().setAll(Option.withAll("Все типы мест", List.of(WorkspaceType.values()), Labels::of));
        floorCombo.getItems().setAll(Option.withAll("Все этажи", List.of(), floor -> "Этаж " + floor));
        statusCombo.getSelectionModel().selectFirst();
        typeCombo.getSelectionModel().selectFirst();
        floorCombo.getSelectionModel().selectFirst();
        sortCombo.getItems().setAll(BookingSort.values());
        sortCombo.setValue(BookingSort.START_TIME);

        Consumer<Object> trigger = value -> reload();
        statusCombo.valueProperty().addListener((obs, oldValue, newValue) -> trigger.accept(newValue));
        typeCombo.valueProperty().addListener((obs, oldValue, newValue) -> trigger.accept(newValue));
        floorCombo.valueProperty().addListener((obs, oldValue, newValue) -> trigger.accept(newValue));
        dateFromPicker.valueProperty().addListener((obs, oldValue, newValue) -> trigger.accept(newValue));
        dateToPicker.valueProperty().addListener((obs, oldValue, newValue) -> trigger.accept(newValue));
        sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> trigger.accept(newValue));
        ascCheck.selectedProperty().addListener((obs, oldValue, newValue) -> trigger.accept(newValue));
    }

    private void configureSelectionButtons() {
        bookingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, row) -> updateButtons(row));
        updateButtons(null);
    }

    private void updateButtons(BookingRow row) {
        boolean none = row == null;
        boolean active = !none && row.status() == BookingStatus.ACTIVE;
        boolean manager = context.session().isManager();
        boolean ownerOrManager = !none && (manager || context.session().owns(row.employeeId()));
        deleteButton.setDisable(none || !manager);
        editButton.setDisable(!active || !ownerOrManager);
        cancelBookingButton.setDisable(!active || !ownerOrManager);
        completeButton.setDisable(!active || !manager);
        noShowButton.setDisable(!active || !manager);
    }

    private void showStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().remove("error");
        if (error) {
            statusLabel.getStyleClass().add("error");
        }
    }

    private Window owner() {
        return bookingTable.getScene() == null ? null : bookingTable.getScene().getWindow();
    }

    private static Path withExtension(File file, FileChooser.ExtensionFilter filter) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".csv")) {
            return file.toPath();
        }
        String extension = filter != null && filter.getExtensions().contains("*.csv") ? ".csv" : ".xlsx";
        return file.toPath().resolveSibling(file.getName() + extension);
    }
}
