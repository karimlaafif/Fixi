package ma.fixi.dev;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import ma.fixi.service.Services;
import ma.fixi.service.mock.MockStore;
import ma.fixi.view.Navigator;
import ma.fixi.view.Route;

/**
 * Outil de développement : parcourt tous les écrans et enregistre une capture PNG de chacun,
 * puis ferme l'application. Utile pour vérifier l'interface sans cliquer partout.
 *
 * <p>Lancement : {@code FIXI_SCREENSHOTS=/chemin/dossier mvn javafx:run}</p>
 */
public class ScreenshotTour {

    private record Step(String name, Runnable action) {
    }

    private final Scene scene;
    private final Path outputDir;
    private final List<Step> steps = new ArrayList<>();
    private int index;

    public ScreenshotTour(Scene scene, Path outputDir) {
        this.scene = scene;
        this.outputDir = outputDir;
        steps.add(new Step("01-login", () -> { }));
        steps.add(new Step("02-register", Navigator::showRegister));
        steps.add(new Step("02b-register-errors", () -> click("Créer mon compte")));
        steps.add(new Step("03-client-home", () -> login("client@fixi.ma")));
        steps.add(new Step("04-search", () -> Navigator.goTo(Route.SEARCH)));
        steps.add(new Step("05-artisan-profile", () -> Navigator.goTo(Route.ARTISAN_PROFILE, MockStore.ARTISAN_ID)));
        steps.add(new Step("06-requests", () -> Navigator.goTo(Route.REQUESTS)));
        steps.add(new Step("06b-request-to-validate", () -> openRequest("Fuite d'eau sous l'évier")));
        steps.add(new Step("06c-confirm-dialog", () -> click("Confirmer la fin des travaux")));
        steps.add(new Step("07-new-request", () -> Navigator.goTo(Route.NEW_REQUEST)));
        steps.add(new Step("07b-new-request-errors", () -> click("Envoyer la demande")));
        steps.add(new Step("08-messages", () -> Navigator.goTo(Route.MESSAGES)));
        steps.add(new Step("09-profile", () -> Navigator.goTo(Route.PROFILE)));
        steps.add(new Step("10-artisan-dashboard", () -> login("artisan@fixi.ma")));
        steps.add(new Step("11-artisan-requests", () -> Navigator.goTo(Route.ARTISAN_REQUESTS)));
        steps.add(new Step("12-agenda-placeholder", () -> Navigator.goTo(Route.AGENDA)));
        steps.add(new Step("13-admin-trades", () -> login("admin@fixi.ma")));
        steps.add(new Step("13b-admin-edit", () -> {
            @SuppressWarnings("unchecked")
            javafx.scene.control.TableView<Object> table =
                    (javafx.scene.control.TableView<Object>) scene.getRoot().lookup(".table-view");
            table.getSelectionModel().select(5);
        }));
    }

    /** Clique le premier bouton visible portant ce texte. */
    private void click(String text) {
        scene.getRoot().lookupAll(".button").stream()
                .filter(n -> n instanceof javafx.scene.control.ButtonBase b && text.equals(b.getText()))
                .map(n -> (javafx.scene.control.ButtonBase) n)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Bouton introuvable : " + text))
                .fire();
    }

    /** Ouvre la demande dont la ligne contient ce titre. */
    private void openRequest(String title) {
        scene.getRoot().lookupAll(".request-row").stream()
                .filter(row -> row.lookupAll(".label").stream()
                        .anyMatch(l -> l instanceof javafx.scene.control.Label lb && title.equals(lb.getText())))
                .findFirst()
                .orElseThrow()
                .getOnMouseClicked().handle(null);
    }

    private static void login(String email) {
        Navigator.showMain(Services.auth().login(email, MockStore.DEMO_PASSWORD));
    }

    public void start() {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        next();
    }

    private void next() {
        if (index >= steps.size()) {
            System.out.println("Captures enregistrées dans " + outputDir.toAbsolutePath());
            Platform.runLater(Platform::exit);
            return;
        }
        Step step = steps.get(index++);
        try {
            step.action().run();
        } catch (RuntimeException e) {
            System.err.println("Étape « " + step.name() + " » en échec :");
            e.printStackTrace();
        }
        PauseTransition wait = new PauseTransition(Duration.millis(700));
        wait.setOnFinished(e -> {
            save(step.name());
            next();
        });
        wait.play();
    }

    private void save(String name) {
        WritableImage image = scene.snapshot(null);
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        try {
            ImageIO.write(out, "png", outputDir.resolve(name + ".png").toFile());
        } catch (IOException e) {
            System.err.println("Capture impossible : " + name + " — " + e.getMessage());
        }
    }
}
