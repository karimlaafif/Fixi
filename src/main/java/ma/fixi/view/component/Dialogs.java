package ma.fixi.view.component;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Fenêtres de dialogue affichées au-dessus de l'écran courant (fond assombri),
 * plutôt que des fenêtres système, pour garder le même style partout.
 * Échap ou un clic sur le fond ferme le dialogue.
 */
public final class Dialogs {

    private Dialogs() {
    }

    /** Demande une confirmation avant une action. {@code danger} colore le bouton en rouge. */
    public static void confirm(Node anyNode, String title, String message, String confirmLabel,
                               boolean danger, Runnable onConfirm) {
        Label text = Ui.label(message, "text-muted");
        text.setWrapText(true);
        Button cancel = Ui.button("Retour", "btn-secondary", null);
        Button ok = Ui.button(confirmLabel, danger ? "btn-danger" : "btn-primary", null);
        Handle h = show(anyNode, title, text, List.of(cancel, ok));
        cancel.setOnAction(e -> h.close());
        ok.setOnAction(e -> {
            h.close();
            onConfirm.run();
        });
        ok.requestFocus();
    }

    /** Dialogue générique : titre, contenu libre, boutons en bas à droite. */
    public static Handle show(Node anyNode, String title, Node content, List<Button> actions) {
        StackPane root = (StackPane) anyNode.getScene().getRoot();
        VBox card = new VBox(16);
        card.getStyleClass().add("dialog-card");
        card.setMaxWidth(480);
        card.setMaxHeight(StackPane.USE_PREF_SIZE);
        Label heading = Ui.label(title, "dialog-title");
        heading.setWrapText(true);
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.getChildren().addAll(actions);
        card.getChildren().addAll(heading, content);
        if (!actions.isEmpty()) {
            card.getChildren().add(buttons);
        }

        StackPane overlay = new StackPane(card);
        overlay.getStyleClass().add("dialog-overlay");
        Handle handle = new Handle(root, overlay);
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                handle.close();
            }
        });
        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                handle.close();
            }
        });
        root.getChildren().add(overlay);
        overlay.requestFocus();
        return handle;
    }

    /** Permet de fermer un dialogue ouvert. */
    public static final class Handle {
        private final StackPane root;
        private final Node overlay;

        private Handle(StackPane root, Node overlay) {
            this.root = root;
            this.overlay = overlay;
        }

        public void close() {
            root.getChildren().remove(overlay);
        }
    }
}
