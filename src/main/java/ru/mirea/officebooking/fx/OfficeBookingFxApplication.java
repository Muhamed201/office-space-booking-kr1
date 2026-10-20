package ru.mirea.officebooking.fx;

import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.fx.controller.LoginController;
import ru.mirea.officebooking.fx.util.Alerts;
import ru.mirea.officebooking.fx.util.Dialogs;
import ru.mirea.officebooking.fx.util.ErrorMessages;
import ru.mirea.officebooking.fx.util.FxmlSupport;

public class OfficeBookingFxApplication extends Application {

    private static final String TITLE = "Учёт офисных рабочих мест";
    private static final double PREFERRED_WIDTH = 1280;
    private static final double PREFERRED_HEIGHT = 800;
    private static final double MIN_WIDTH = 900;
    private static final double MIN_HEIGHT = 560;
    private static final double SCREEN_SHARE = 0.9;

    private AppContext context;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        this.stage = primaryStage;
        try {
            context = new AppContext();
        } catch (AppException e) {
            Alerts.error(null, "Не удалось запустить приложение", ErrorMessages.of(e));
            Platform.exit();
            return;
        }
        stage.setTitle(TITLE);
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        stage.setMinWidth(Math.min(MIN_WIDTH, area.getWidth()));
        stage.setMinHeight(Math.min(MIN_HEIGHT, area.getHeight()));
        stage.setOnHidden(event -> Platform.exit());
        context.session().setLogoutHandler(this::loginAndShowMain);
        loginAndShowMain();
    }

    private void loginAndShowMain() {
        Window owner = stage.isShowing() ? stage : null;
        if (!Dialogs.showForm(owner, "Вход", "login.fxml", new LoginController(context), "Войти")) {
            Platform.exit();
            return;
        }
        try {
            Parent root = FxmlSupport.load("main.fxml", FxmlSupport.factoryFor(context));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(FxmlSupport.stylesheet());
            boolean firstShow = !stage.isShowing();
            stage.setScene(scene);
            if (firstShow) {
                placeOnScreen();
                stage.show();
            }
        } catch (IOException e) {
            Alerts.error(owner, "Не удалось открыть главное окно", ErrorMessages.of(e));
            Platform.exit();
        }
    }

    private void placeOnScreen() {
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        double width = Math.min(PREFERRED_WIDTH, area.getWidth() * SCREEN_SHARE);
        double height = Math.min(PREFERRED_HEIGHT, area.getHeight() * SCREEN_SHARE);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(area.getMinX() + (area.getWidth() - width) / 2);
        stage.setY(area.getMinY() + (area.getHeight() - height) / 2);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
