package ma.fixi.controller;

import java.util.EnumMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ma.fixi.model.Role;
import ma.fixi.model.User;
import ma.fixi.view.Navigator;
import ma.fixi.view.Route;
import ma.fixi.view.Session;
import ma.fixi.view.component.Ui;

/** Coque de l'espace connecté : construit le menu selon le rôle et affiche la page demandée. */
public class ShellController {

    @FXML private VBox navBox;
    @FXML private Label roleLabel;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private StackPane searchBox;
    @FXML private TextField globalSearch;
    @FXML private MenuButton userMenu;
    @FXML private StackPane content;

    private final Map<Route, Button> navButtons = new EnumMap<>(Route.class);
    private Role role;

    @FXML
    private void initialize() {
        User user = Session.user();
        role = user.getPrimaryRole();
        roleLabel.setText(switch (role) {
            case ARTISAN -> "ESPACE ARTISAN";
            case ADMINISTRATOR, SUPPORT -> "ADMINISTRATION";
            case CLIENT -> "ESPACE CLIENT";
        });

        for (Route route : Route.menuFor(role)) {
            Button b = new Button(route.getMenuLabel(), Ui.icon(route.getIcon(), 19));
            b.getStyleClass().add("nav-item");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> Navigator.goTo(route));
            navButtons.put(route, b);
            navBox.getChildren().add(b);
        }

        refreshUserMenu();
    }

    /** Met à jour l'avatar et le nom affichés en haut à droite (après modification du profil). */
    public void refreshUserMenu() {
        User user = Session.user();
        Label name = Ui.label(user.getFullName(), "semibold");
        Label sub = Ui.label(role.getLabel(), "text-subtle");
        VBox text = new VBox(0, name, sub);
        HBox graphic = new HBox(10, Ui.avatar(user.getFullName(), 38, false), text);
        graphic.setAlignment(Pos.CENTER_LEFT);
        userMenu.setGraphic(graphic);
    }

    /** Appelé par {@link Navigator} pour afficher une page. */
    public void showPage(Route route, Parent page) {
        setHeader(route.getTitle(), route.getSubtitle());
        boolean showSearch = role == Role.CLIENT && route != Route.SEARCH;
        searchBox.setVisible(showSearch);
        searchBox.setManaged(showSearch);
        globalSearch.clear();
        navButtons.forEach((r, b) -> b.getStyleClass().remove("active"));
        Button active = navButtons.get(route.menuEntry());
        if (active != null) {
            active.getStyleClass().add("active");
        }
        content.getChildren().setAll(page);
    }

    /** Les pages peuvent remplacer le titre par défaut (par exemple « Bonjour, Sarah ! »). */
    public void setHeader(String title, String subtitle) {
        titleLabel.setText(title);
        boolean hasSubtitle = subtitle != null && !subtitle.isBlank();
        subtitleLabel.setText(hasSubtitle ? subtitle : "");
        subtitleLabel.setVisible(hasSubtitle);
        subtitleLabel.setManaged(hasSubtitle);
    }

    @FXML
    private void onGlobalSearch() {
        String query = globalSearch.getText();
        Navigator.goTo(Route.SEARCH, query == null || query.isBlank() ? null : query.trim());
    }

    @FXML
    private void onProfile() {
        Navigator.goTo(Route.PROFILE);
    }

    @FXML
    private void onLogout() {
        Navigator.showLogin();
    }
}
