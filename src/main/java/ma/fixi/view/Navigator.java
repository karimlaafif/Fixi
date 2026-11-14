package ma.fixi.view;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ma.fixi.controller.ShellController;
import ma.fixi.model.User;

/**
 * Navigation entre les écrans. Une seule fenêtre et une seule scène :
 * seul le contenu change.
 *
 * <ul>
 *   <li>{@link #showLogin()} / {@link #showRegister()} : écrans d'authentification plein écran ;</li>
 *   <li>{@link #showMain(User)} : espace connecté (menu latéral + barre du haut) ;</li>
 *   <li>{@link #goTo(Route, Object)} : change la page affichée dans l'espace connecté.</li>
 * </ul>
 */
public final class Navigator {

    public static final String STYLESHEET = "/css/fixi.css";

    private static Stage stage;
    private static Scene scene;
    private static ShellController shell;
    private static final Deque<Entry> history = new ArrayDeque<>();

    private record Entry(Route route, Object param) {
    }

    private Navigator() {
    }

    public static void init(Stage primaryStage, Scene primaryScene) {
        stage = primaryStage;
        scene = primaryScene;
        scene.getStylesheets().add(Navigator.class.getResource(STYLESHEET).toExternalForm());
    }

    public static Stage stage() {
        return stage;
    }

    public static void showLogin() {
        Session.end();
        shell = null;
        history.clear();
        setRoot(load("login.fxml").root());
    }

    public static void showRegister() {
        setRoot(load("register.fxml").root());
    }

    /** Ouvre l'espace connecté sur la page d'accueil correspondant au rôle de l'utilisateur. */
    public static void showMain(User user) {
        Session.start(user);
        Loaded loaded = load("shell.fxml");
        shell = (ShellController) loaded.controller();
        history.clear();
        setRoot(loaded.root());
        goTo(Route.homeFor(user.getPrimaryRole()));
    }

    public static void goTo(Route route) {
        goTo(route, null);
    }

    public static void goTo(Route route, Object param) {
        if (shell == null) {
            throw new IllegalStateException("L'espace connecté n'est pas affiché.");
        }
        history.push(new Entry(route, param));
        display(route, param);
    }

    /** Revient à la page précédente (bouton « ← »). */
    public static void back() {
        if (history.size() > 1) {
            history.pop();
            Entry previous = history.peek();
            display(previous.route(), previous.param());
        }
    }

    /** Remplace le titre et le sous-titre de la barre du haut pour la page courante. */
    public static void setHeader(String title, String subtitle) {
        if (shell != null) {
            shell.setHeader(title, subtitle);
        }
    }

    /** Met à jour le nom affiché en haut à droite après une modification du profil. */
    public static void refreshUser() {
        if (shell != null) {
            shell.refreshUserMenu();
        }
    }

    /** Page actuellement affichée. */
    public static Route currentRoute() {
        return history.isEmpty() ? null : history.peek().route();
    }

    private static void display(Route route, Object param) {
        // Un dialogue ou un message resté ouvert ne doit pas suivre l'utilisateur sur une autre page.
        if (scene.getRoot() instanceof StackPane root) {
            root.getChildren().removeIf(n -> n.getStyleClass().contains("dialog-overlay"));
        }
        Loaded page = load(route.getFxml());
        shell.showPage(route, page.root());
        if (page.controller() instanceof Page p) {
            p.onShow(route, param);
        }
    }

    private static void setRoot(Parent content) {
        StackPane appRoot = new StackPane(content);
        appRoot.getStyleClass().add("app-root");
        scene.setRoot(appRoot);
    }

    /** Charge un fichier de {@code src/main/resources/view/}. */
    public static Loaded load(String fxml) {
        FXMLLoader loader = new FXMLLoader(Navigator.class.getResource("/view/" + fxml));
        try {
            Parent root = loader.load();
            return new Loaded(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de charger /view/" + fxml, e);
        }
    }

    public record Loaded(Parent root, Object controller) {
    }
}
