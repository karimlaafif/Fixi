package ma.fixi.controller;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ma.fixi.model.ServiceRequest;
import ma.fixi.model.Urgency;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.util.Formats;
import ma.fixi.view.Session;
import ma.fixi.view.component.Dialogs;
import ma.fixi.view.component.Toast;
import ma.fixi.view.component.Ui;

/** Ligne « demande reçue » de l'espace artisan, avec ses actions (voir, décliner). */
final class IncomingRequests {

    private IncomingRequests() {
    }

    /**
     * @param onChanged appelé après un refus, pour recharger la liste
     */
    static HBox row(ServiceRequest r, Node anchor, Runnable onChanged) {
        Label title = Ui.label(r.getTitle(), "request-title");
        javafx.scene.layout.FlowPane meta = new javafx.scene.layout.FlowPane(16, 4, Ui.iconText("fth-map-pin", r.getCity().getNameFr(), "text-small"),
                Ui.iconText("fth-calendar", "Souhaité le " + Formats.date(r.getPreferredDate()), "text-small"),
                Ui.iconText("fth-user", r.getClientName(), "text-small"));
        VBox text = new VBox(5, title, meta);
        HBox.setHgrow(text, Priority.ALWAYS);
        text.setMinWidth(0);

        HBox row = new HBox(14, ma.fixi.view.component.Images.trade(r.getTrade(), 44), text);
        if (r.getUrgency() == Urgency.URGENT) {
            row.getChildren().add(Ui.badge("Urgent", "badge-danger"));
        } else {
            row.getChildren().add(Ui.badge("Nouvelle", "badge-primary"));
        }
        Button view = Ui.button("Voir", "btn-primary", null);
        view.getStyleClass().add("btn-small");
        view.setOnAction(e -> showDetails(r, anchor, onChanged));
        row.getChildren().add(view);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("request-row");
        row.setOnMouseClicked(e -> showDetails(r, anchor, onChanged));
        return row;
    }

    static void showDetails(ServiceRequest r, Node anchor, Runnable onChanged) {
        Label description = new Label(r.getDescription());
        description.setWrapText(true);
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(8);
        String[][] facts = {
                {"Client", r.getClientName()},
                {"Ville", r.getCity().getNameFr()},
                {"Date souhaitée", Formats.date(r.getPreferredDate())},
                {"Budget indicatif", r.getBudget() == null ? "Non précisé" : Formats.mad(r.getBudget())},
                {"Urgence", r.getUrgency().getLabel()}};
        for (int i = 0; i < facts.length; i++) {
            grid.add(Ui.label(facts[i][0], "kv-key"), 0, i);
            grid.add(Ui.label(facts[i][1], "kv-value"), 1, i);
        }
        Label note = Ui.label("La création de devis détaillés arrive dans la prochaine itération.", "text-subtle");
        note.setWrapText(true);
        VBox content = new VBox(14, Ui.label(r.getReference() + " · " + r.getTrade().getNameFr(), "text-muted"),
                description, grid, note);

        Button decline = Ui.button("Décliner", "btn-danger-outline", null);
        Button close = Ui.button("Fermer", "btn-secondary", null);
        Button quote = Ui.button("Proposer un devis", "btn-primary", "fth-file-plus");
        quote.setDisable(true);
        Dialogs.Handle h = Dialogs.show(anchor, r.getTitle(), content, List.of(decline, close, quote));
        close.setOnAction(e -> h.close());
        decline.setOnAction(e -> {
            h.close();
            Dialogs.confirm(anchor, "Décliner cette demande ?", "Elle disparaîtra de votre liste et le client "
                    + "ne recevra pas de devis de votre part.", "Décliner", true, () -> {
                String artisanId = Session.user().getId();
                Async.run(() -> Services.requests().decline(artisanId, r.getId()), () -> {
                    Toast.info(anchor, "Demande déclinée.");
                    onChanged.run();
                }, err -> Toast.error(anchor, Async.userMessage(err)));
            });
        });
    }
}
