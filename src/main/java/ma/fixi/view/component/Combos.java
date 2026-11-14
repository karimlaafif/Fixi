package ma.fixi.view.component;

import java.util.function.Function;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

/** Réglages communs des listes déroulantes. */
public final class Combos {

    private Combos() {
    }

    /**
     * Affiche chaque élément avec {@code label}, et {@code nullLabel} pour la valeur {@code null}
     * (par exemple « Tous les métiers »). Ajoutez {@code null} en tête des éléments pour le proposer.
     */
    public static <T> void labels(ComboBox<T> combo, Function<T, String> label, String nullLabel) {
        if (nullLabel != null) {
            combo.setPromptText(nullLabel);
        }
        combo.setButtonCell(new Cell<>(label, nullLabel, combo.getPromptText()));
        combo.setCellFactory(lv -> new Cell<>(label, nullLabel, null));
    }

    private static final class Cell<T> extends ListCell<T> {
        private final Function<T, String> label;
        private final String nullLabel;
        private final String prompt;

        Cell(Function<T, String> label, String nullLabel, String prompt) {
            this.label = label;
            this.nullLabel = nullLabel;
            this.prompt = prompt;
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null && !empty) {
                setText(label.apply(item));
            } else if (nullLabel != null) {
                setText(nullLabel);
            } else {
                setText(prompt);
            }
            getStyleClass().remove("prompt");
            if ((item == null || empty) && nullLabel == null && prompt != null) {
                getStyleClass().add("prompt");
            }
        }
    }
}
