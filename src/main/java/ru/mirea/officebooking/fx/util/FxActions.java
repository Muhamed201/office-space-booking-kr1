package ru.mirea.officebooking.fx.util;

import javafx.stage.Window;

public final class FxActions {

    private FxActions() {
    }

    public static void run(Window owner, Runnable action, Runnable afterwards) {
        AsyncRunner.run(
                () -> {
                    action.run();
                    return Boolean.TRUE;
                },
                done -> afterwards.run(),
                error -> {
                    Alerts.error(owner, "Операция не выполнена", ErrorMessages.of(error));
                    afterwards.run();
                });
    }
}
