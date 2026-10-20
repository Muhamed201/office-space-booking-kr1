package ru.mirea.officebooking.fx.util;

import java.time.LocalDate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

public final class InputRules {

    public static final String INVALID = "invalid";

    private static final Pattern ANY = Pattern.compile("(?s).*");
    private static final Pattern CYRILLIC_NAME = Pattern.compile("[А-Яа-яЁё -]*");
    private static final Pattern EMAIL_CHARS = Pattern.compile("[A-Za-z0-9._%+@-]*");
    private static final Pattern WORKSPACE_CODE = Pattern.compile("[A-Z]{0,3}(-\\d{0,3})?");

    private InputRules() {
    }

    public static void maxLength(TextInputControl field, int maxLength) {
        allow(field, ANY, maxLength, UnaryOperator.identity());
    }

    public static void digits(TextInputControl field, int maxDigits) {
        allow(field, Pattern.compile("\\d{0,%d}".formatted(maxDigits)), maxDigits, UnaryOperator.identity());
    }

    public static void signedDigits(TextInputControl field, int maxDigits) {
        allow(field, Pattern.compile("-?\\d{0,%d}".formatted(maxDigits)), maxDigits + 1, UnaryOperator.identity());
    }

    public static void personName(TextInputControl field, int maxLength) {
        allow(field, CYRILLIC_NAME, maxLength, UnaryOperator.identity());
    }

    public static void email(TextInputControl field, int maxLength) {
        allow(field, EMAIL_CHARS, maxLength, text -> text.toLowerCase());
    }

    public static void workspaceCode(TextInputControl field, int maxLength) {
        allow(field, WORKSPACE_CODE, maxLength, text -> text.toUpperCase());
    }

    public static void dateOnly(DatePicker picker, Supplier<LocalDate> min, Supplier<LocalDate> max) {
        picker.setEditable(false);
        picker.setDayCellFactory(view -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate from = min.get();
                LocalDate to = max.get();
                boolean outside = date != null
                        && ((from != null && date.isBefore(from)) || (to != null && date.isAfter(to)));
                setDisable(empty || outside);
                getStyleClass().remove("date-unavailable");
                if (outside) {
                    getStyleClass().add("date-unavailable");
                }
            }
        });
    }

    public static void dateRange(DatePicker from, DatePicker to) {
        dateOnly(from, () -> null, to::getValue);
        dateOnly(to, from::getValue, () -> null);
    }

    public static void markInvalid(Control control) {
        if (!control.getStyleClass().contains(INVALID)) {
            control.getStyleClass().add(INVALID);
        }
        control.requestFocus();
    }

    public static void clearInvalid(Control... controls) {
        for (Control control : controls) {
            control.getStyleClass().remove(INVALID);
        }
    }

    private static void allow(TextInputControl field, Pattern pattern, int maxLength, UnaryOperator<String> transform) {
        field.setTextFormatter(new TextFormatter<String>(change -> {
            if (change.isContentChange()) {
                change.setText(transform.apply(change.getText()));
            }
            String next = change.getControlNewText();
            return next.length() <= maxLength && pattern.matcher(next).matches() ? change : null;
        }));
        field.textProperty().addListener((obs, oldValue, newValue) -> field.getStyleClass().remove(INVALID));
    }
}
