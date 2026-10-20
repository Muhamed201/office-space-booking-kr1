package ru.mirea.officebooking.fx.util;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.util.Callback;
import ru.mirea.officebooking.fx.AppContext;

public final class FxmlSupport {

    private static final String FXML_DIR = "/fxml/";
    private static final String STYLESHEET = "/css/app.css";

    private FxmlSupport() {
    }

    public static Parent load(String fxmlName, Callback<Class<?>, Object> controllerFactory) throws IOException {
        URL location = FxmlSupport.class.getResource(FXML_DIR + fxmlName);
        if (location == null) {
            throw new IOException("Не найден файл разметки " + fxmlName);
        }
        FXMLLoader loader = new FXMLLoader(location);
        loader.setControllerFactory(controllerFactory);
        return loader.load();
    }

    public static Callback<Class<?>, Object> factoryFor(AppContext context) {
        return type -> {
            try {
                return type.getConstructor(AppContext.class).newInstance(context);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Не удалось создать контроллер " + type.getSimpleName(), e);
            }
        };
    }

    public static Callback<Class<?>, Object> factoryFor(Object controller) {
        return type -> controller;
    }

    public static String stylesheet() {
        URL url = FxmlSupport.class.getResource(STYLESHEET);
        if (url == null) {
            throw new IllegalStateException("Не найден файл стилей " + STYLESHEET);
        }
        return url.toExternalForm();
    }
}
