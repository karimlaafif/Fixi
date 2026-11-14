package ma.fixi.view.component;

import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Champ mot de passe avec icône cadenas et bouton « afficher / masquer ».
 * Utilisable dans un FXML : {@code <PasswordInput fx:id="password" promptText="…"/>}.
 */
public class PasswordInput extends StackPane {

    private final PasswordField hidden = new PasswordField();
    private final TextField visible = new TextField();
    private final FontIcon eye = Ui.icon("fth-eye", 17, "icon-muted");
    private final Button toggle = new Button();

    public PasswordInput() {
        getStyleClass().add("password-input");
        visible.textProperty().bindBidirectional(hidden.textProperty());
        visible.promptTextProperty().bind(hidden.promptTextProperty());
        visible.setVisible(false);
        visible.setManaged(false);
        for (TextField f : new TextField[]{hidden, visible}) {
            f.getStyleClass().addAll("field-with-icon", "field-with-trailing");
            f.setMaxWidth(Double.MAX_VALUE);
        }

        FontIcon lock = Ui.icon("fth-lock", 17, "icon-muted");
        lock.setMouseTransparent(true);
        StackPane.setAlignment(lock, Pos.CENTER_LEFT);
        StackPane.setMargin(lock, new Insets(0, 0, 0, 14));

        toggle.setGraphic(eye);
        toggle.getStyleClass().add("btn-icon-inline");
        toggle.setCursor(Cursor.HAND);
        toggle.setFocusTraversable(false);
        toggle.setTooltip(new Tooltip("Afficher le mot de passe"));
        toggle.setOnAction(e -> setRevealed(!visible.isVisible()));
        StackPane.setAlignment(toggle, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggle, new Insets(0, 6, 0, 0));

        getChildren().addAll(hidden, visible, lock, toggle);
    }

    private void setRevealed(boolean revealed) {
        TextField from = revealed ? hidden : visible;
        TextField to = revealed ? visible : hidden;
        to.setVisible(true);
        to.setManaged(true);
        from.setVisible(false);
        from.setManaged(false);
        eye.setIconLiteral(revealed ? "fth-eye-off" : "fth-eye");
        toggle.getTooltip().setText(revealed ? "Masquer le mot de passe" : "Afficher le mot de passe");
        to.requestFocus();
        to.end();
    }

    public String getText() {
        return hidden.getText();
    }

    public void setText(String text) {
        hidden.setText(text);
    }

    public StringProperty textProperty() {
        return hidden.textProperty();
    }

    public String getPromptText() {
        return hidden.getPromptText();
    }

    public void setPromptText(String prompt) {
        hidden.setPromptText(prompt);
    }

    /** Ajoute ou retire l'état d'erreur (bordure rouge). */
    public void setInvalid(boolean invalid) {
        for (TextField f : new TextField[]{hidden, visible}) {
            f.getStyleClass().remove("invalid");
            if (invalid) {
                f.getStyleClass().add("invalid");
            }
        }
    }

    /** Champ actif (pour la touche Entrée ou le focus). */
    public TextField activeField() {
        return visible.isVisible() ? visible : hidden;
    }
}
