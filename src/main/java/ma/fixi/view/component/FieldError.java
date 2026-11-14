package ma.fixi.view.component;

import javafx.scene.Node;
import javafx.scene.control.Label;

/** Affiche ou masque le message d'erreur sous un champ, et la bordure rouge du champ. */
public final class FieldError {

    private FieldError() {
    }

    /**
     * @param message {@code null} pour effacer l'erreur
     * @return vrai si une erreur est affichée (pratique pour compter les erreurs)
     */
    public static boolean set(Node field, Label errorLabel, String message) {
        boolean invalid = message != null;
        errorLabel.setText(invalid ? message : "");
        errorLabel.setVisible(invalid);
        errorLabel.setManaged(invalid);
        if (field instanceof PasswordInput p) {
            p.setInvalid(invalid);
        } else if (field != null) {
            field.getStyleClass().remove("invalid");
            if (invalid) {
                field.getStyleClass().add("invalid");
            }
        }
        return invalid;
    }
}
