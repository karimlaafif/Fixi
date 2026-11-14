package ma.fixi.view.component;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Message bref affiché en bas de la fenêtre (« Demande envoyée »), qui disparaît tout seul. */
public final class Toast {

    private Toast() {
    }

    public static void success(Node anyNode, String message) {
        show(anyNode, message, "toast-success", "fth-check-circle");
    }

    public static void error(Node anyNode, String message) {
        show(anyNode, message, "toast-error", "fth-alert-circle");
    }

    public static void info(Node anyNode, String message) {
        show(anyNode, message, "toast-info", "fth-info");
    }

    private static void show(Node anyNode, String message, String styleClass, String iconLiteral) {
        if (anyNode == null || anyNode.getScene() == null || !(anyNode.getScene().getRoot() instanceof StackPane root)) {
            return;
        }
        root.getChildren().removeIf(n -> n.getStyleClass().contains("toast"));
        Label toast = new Label(message, Ui.icon(iconLiteral, 16));
        toast.setGraphicTextGap(10);
        toast.getStyleClass().addAll("toast", styleClass);
        toast.setMaxWidth(520);
        toast.setWrapText(true);
        toast.setMouseTransparent(true);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 0, 28, 0));
        root.getChildren().add(toast);

        FadeTransition in = new FadeTransition(Duration.millis(150), toast);
        in.setFromValue(0);
        in.setToValue(1);
        FadeTransition out = new FadeTransition(Duration.millis(300), toast);
        out.setToValue(0);
        SequentialTransition seq = new SequentialTransition(in, new PauseTransition(Duration.seconds(3.2)), out);
        seq.setOnFinished(e -> root.getChildren().remove(toast));
        seq.play();
    }
}
