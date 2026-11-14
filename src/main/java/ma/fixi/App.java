package ma.fixi;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import ma.fixi.config.ServiceConfig;
import ma.fixi.view.Navigator;

/**
 * Point d'entrée de l'application.
 * Lancement : {@code mvn javafx:run}
 */
public class App extends Application {

    private static final String[] FONT_FILES = {
            "PlusJakartaSans-Regular.ttf", "PlusJakartaSans-Medium.ttf", "PlusJakartaSans-SemiBold.ttf",
            "PlusJakartaSans-Bold.ttf", "PlusJakartaSans-ExtraBold.ttf"};

    @Override
    public void start(Stage stage) {
        java.util.Locale.setDefault(java.util.Locale.FRANCE);
        loadFonts();
        ServiceConfig.install();

        Scene scene = new Scene(new StackPane(), 1320, 840);
        Navigator.init(stage, scene);
        stage.setTitle("Fixi");
        var icon = ma.fixi.view.component.Images.load(ma.fixi.view.component.Images.LOGO);
        if (icon != null) {
            stage.getIcons().add(icon);
        }
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setScene(scene);
        Navigator.showLogin();
        stage.show();

        String screenshotDir = System.getenv("FIXI_SCREENSHOTS");
        if (screenshotDir != null && !screenshotDir.isBlank()) {
            new ma.fixi.dev.ScreenshotTour(scene, java.nio.file.Path.of(screenshotDir)).start();
        }
    }

    /** Charge la police Plus Jakarta Sans fournie dans src/main/resources/fonts. */
    private static void loadFonts() {
        for (String file : FONT_FILES) {
            try (var in = App.class.getResourceAsStream("/fonts/" + file)) {
                if (in != null) {
                    Font.loadFont(in, 14);
                }
            } catch (java.io.IOException e) {
                System.err.println("Police introuvable : " + file);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
