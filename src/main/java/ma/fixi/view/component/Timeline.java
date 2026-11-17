package ma.fixi.view.component;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ma.fixi.model.RequestStatus;

/** Frise de suivi d'une demande (« Demande envoyée → … → Évaluée »). */
public final class Timeline {

    private Timeline() {
    }

    public static HBox of(RequestStatus status) {
        List<String> steps = RequestStatus.TIMELINE_STEPS;
        int current = status.getTimelineStep();
        boolean allDone = status == RequestStatus.CLOSED;
        // Une étape « terminée » validée par le client compte comme faite.
        int doneUntil = allDone ? steps.size() - 1 : (status.isFinished() ? current : current - 1);

        HBox row = new HBox();
        row.setAlignment(Pos.TOP_CENTER);
        for (int i = 0; i < steps.size(); i++) {
            boolean done = i <= doneUntil;
            boolean isCurrent = !done && i == current;

            // Un trait est coloré quand l'étape vers laquelle il mène est atteinte.
            int reached = allDone ? steps.size() - 1 : current;
            Region left = line(i <= reached, i == 0);
            Region right = line(i + 1 <= reached, i == steps.size() - 1);
            HBox track = new HBox(left, dot(done, isCurrent), right);
            track.setAlignment(Pos.CENTER);

            Label label = new Label(steps.get(i));
            label.getStyleClass().add("timeline-label");
            if (done) label.getStyleClass().add("done");
            if (isCurrent) label.getStyleClass().add("current");
            label.setWrapText(true);
            label.setMaxWidth(110);

            VBox column = new VBox(10, track, label);
            column.setAlignment(Pos.TOP_CENTER);
            HBox.setHgrow(column, Priority.ALWAYS);
            column.setPrefWidth(0);
            column.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().add(column);
        }
        return row;
    }

    private static Region line(boolean done, boolean hidden) {
        Region r = new Region();
        r.getStyleClass().add("timeline-line");
        if (done) r.getStyleClass().add("done");
        if (hidden) r.setVisible(false);
        HBox.setHgrow(r, Priority.ALWAYS);
        r.setMinWidth(10);
        return r;
    }

    private static StackPane dot(boolean done, boolean current) {
        StackPane dot = new StackPane();
        dot.getStyleClass().add("timeline-dot");
        dot.setMinSize(28, 28);
        dot.setMaxSize(28, 28);
        if (done) {
            dot.getStyleClass().add("done");
            dot.getChildren().add(Ui.icon("fth-check", 15));
        } else if (current) {
            dot.getStyleClass().add("current");
            Region inner = new Region();
            inner.getStyleClass().add("inner");
            inner.setMaxSize(10, 10);
            inner.setMinSize(10, 10);
            dot.getChildren().add(inner);
        }
        return dot;
    }
}
