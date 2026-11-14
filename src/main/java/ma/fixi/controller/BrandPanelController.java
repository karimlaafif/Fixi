package ma.fixi.controller;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Panneau de marque des écrans de connexion : l'illustration suit la taille du panneau
 * et disparaît quand la fenêtre est trop étroite pour ne pas recouvrir le texte.
 */
public class BrandPanelController {

    @FXML private StackPane root;
    @FXML private ImageView mascot;

    @FXML
    private void initialize() {
        // Coins arrondis : l'illustration ne déborde pas du panneau.
        Rectangle clip = new Rectangle();
        clip.setArcWidth(48);
        clip.setArcHeight(48);
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        root.widthProperty().addListener((o, a, b) -> resize());
        root.heightProperty().addListener((o, a, b) -> resize());
    }

    private void resize() {
        double w = root.getWidth();
        double h = root.getHeight();
        boolean roomy = w >= 560 && h >= 620;
        mascot.setVisible(roomy);
        mascot.setManaged(roomy);
        // Environ la moitié de la largeur, sans dépasser 45 % de la hauteur.
        mascot.setFitWidth(Math.min(w * 0.46, h * 0.42 * 0.95));
    }
}
