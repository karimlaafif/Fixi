package ma.fixi.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ma.fixi.view.Page;
import ma.fixi.view.Route;
import ma.fixi.view.component.Ui;

/**
 * Page affichée pour une fonctionnalité prévue dans une itération suivante.
 * Elle dit clairement ce qui viendra, au lieu d'afficher de fausses données.
 */
public class PlaceholderController implements Page {

    @FXML private StackPane root;

    @Override
    public void onShow(Route route, Object param) {
        String message = switch (route) {
            case AGENDA -> "Vous retrouverez ici vos rendez-vous, vos créneaux de disponibilité et les rappels. "
                    + "En attendant, vos prochaines interventions sont visibles sur le tableau de bord.";
            case ADMIN_CITIES -> "La gestion des villes suivra le même modèle que celle des métiers.";
            case ADMIN_ARTISANS -> "Vous pourrez consulter les justificatifs (identité, ICE, assurance) "
                    + "puis valider ou suspendre les profils d'artisans.";
            default -> "Cette page sera disponible dans une prochaine version.";
        };
        VBox card = Ui.emptyState(route.getIcon() == null ? "fth-clock" : route.getIcon(),
                route.getTitle() + " arrive bientôt", message, null);
        card.getStyleClass().add("card");
        card.setMaxSize(560, javafx.scene.layout.Region.USE_PREF_SIZE);
        root.getChildren().setAll(card);
        StackPane.setAlignment(card, javafx.geometry.Pos.TOP_CENTER);
    }
}
