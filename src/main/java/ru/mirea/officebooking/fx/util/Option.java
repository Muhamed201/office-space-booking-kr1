package ru.mirea.officebooking.fx.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record Option<T>(String label, T value) {

    @Override
    public String toString() {
        return label;
    }

    public static <T> List<Option<T>> withAll(String allLabel, List<T> values, Function<T, String> labeler) {
        List<Option<T>> options = new ArrayList<>();
        options.add(new Option<>(allLabel, null));
        for (T value : values) {
            options.add(new Option<>(labeler.apply(value), value));
        }
        return options;
    }
}
