package ru.mirea.officebooking.fx.controller;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.mirea.officebooking.dto.StatisticsReport;
import ru.mirea.officebooking.dto.StatisticsReport.NamedCount;
import ru.mirea.officebooking.fx.AppContext;
import ru.mirea.officebooking.fx.util.AsyncRunner;
import ru.mirea.officebooking.fx.util.ErrorMessages;
import ru.mirea.officebooking.fx.util.InputRules;
import ru.mirea.officebooking.fx.util.Labels;

public class StatisticsController {

    private final AppContext context;

    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;
    @FXML private Label totalLabel;
    @FXML private Label periodLabel;
    @FXML private Label noShowLabel;
    @FXML private Label avgLabel;
    @FXML private Label minLabel;
    @FXML private Label maxLabel;
    @FXML private Label employeesLabel;
    @FXML private Label errorLabel;
    @FXML private PieChart statusChart;
    @FXML private PieChart typeChart;
    @FXML private TableView<NamedCount> topWorkspacesTable;
    @FXML private TableColumn<NamedCount, String> workspaceNameColumn;
    @FXML private TableColumn<NamedCount, String> workspaceCountColumn;
    @FXML private TableView<NamedCount> topEmployeesTable;
    @FXML private TableColumn<NamedCount, String> employeeNameColumn;
    @FXML private TableColumn<NamedCount, String> employeeCountColumn;

    public StatisticsController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        InputRules.dateRange(fromPicker, toPicker);
        statusChart.setAnimated(false);
        typeChart.setAnimated(false);
        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        bind(workspaceNameColumn, NamedCount::label);
        bind(workspaceCountColumn, row -> String.valueOf(row.count()));
        bind(employeeNameColumn, NamedCount::label);
        bind(employeeCountColumn, row -> String.valueOf(row.count()));
        onCalculate();
    }

    @FXML
    private void onCalculate() {
        errorLabel.setText("");
        var from = fromPicker.getValue();
        var to = toPicker.getValue();
        AsyncRunner.run(
                () -> context.statistics().buildReport(from, to),
                this::show,
                error -> errorLabel.setText(ErrorMessages.of(error)));
    }

    @FXML
    private void onResetPeriod() {
        fromPicker.setValue(null);
        toPicker.setValue(null);
        onCalculate();
    }

    private void show(StatisticsReport report) {
        totalLabel.setText(String.valueOf(report.totalBookings()));
        periodLabel.setText(String.valueOf(report.bookingsInPeriod()));
        noShowLabel.setText("%.1f %%".formatted(report.noShowPercent()));
        avgLabel.setText("%.1f ч".formatted(report.averageDurationHours()));
        minLabel.setText("%.1f ч".formatted(report.minDurationHours()));
        maxLabel.setText("%.1f ч".formatted(report.maxDurationHours()));
        employeesLabel.setText("%d (заблокировано: %d)".formatted(report.totalEmployees(), report.blockedEmployees()));

        fillChart(statusChart, report.byStatus(), Labels::of);
        fillChart(typeChart, report.byWorkspaceType(), Labels::of);
        topWorkspacesTable.setItems(FXCollections.observableArrayList(report.topWorkspaces()));
        topEmployeesTable.setItems(FXCollections.observableArrayList(report.topEmployees()));
    }

    private static <K> void fillChart(PieChart chart, Map<K, Long> values, Function<K, String> labeler) {
        List<PieChart.Data> slices = chart.getData();
        int index = 0;
        for (Map.Entry<K, Long> entry : values.entrySet()) {
            String name = "%s (%d)".formatted(labeler.apply(entry.getKey()), entry.getValue());
            if (index < slices.size()) {
                slices.get(index).setName(name);
                slices.get(index).setPieValue(entry.getValue());
            } else {
                slices.add(new PieChart.Data(name, entry.getValue()));
            }
            index++;
        }
        while (slices.size() > index) {
            slices.remove(slices.size() - 1);
        }
    }

    private static void bind(TableColumn<NamedCount, String> column, Function<NamedCount, String> getter) {
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
    }
}
