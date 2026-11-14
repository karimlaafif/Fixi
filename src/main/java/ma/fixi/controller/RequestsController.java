package ma.fixi.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DateCell;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ma.fixi.model.RequestStatus;
import ma.fixi.model.ServiceRequest;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.util.Formats;
import ma.fixi.view.Navigator;
import ma.fixi.view.Page;
import ma.fixi.view.Route;
import ma.fixi.view.Session;
import ma.fixi.view.component.Cards;
import ma.fixi.view.component.Dialogs;
import ma.fixi.view.component.Timeline;
import ma.fixi.view.component.Toast;
import ma.fixi.view.component.Ui;

/**
 * Mes demandes : liste filtrable et détail de la demande sélectionnée.
 * Les boutons d'action n'apparaissent que si le statut les autorise (voir {@link RequestStatus}).
 */
public class RequestsController implements Page {

    @FXML private ToggleGroup filterGroup;
    @FXML private ToggleButton allFilter;
    @FXML private ToggleButton activeFilter;
    @FXML private ToggleButton doneFilter;
    @FXML private VBox listBox;
    @FXML private VBox detailBox;
    @FXML private HBox actionsBar;

    private List<ServiceRequest> requests = new ArrayList<>();
    private ServiceRequest selected;

    @FXML
    private void initialize() {
        filterGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                filterGroup.selectToggle(old);
            } else {
                renderList();
            }
        });
    }

    @Override
    public void onShow(Route route, Object param) {
        load(param instanceof String id ? id : null);
    }

    private void load(String selectId) {
        String clientId = Session.user().getId();
        Async.run(() -> Services.requests().findByClient(clientId), list -> {
            requests = new ArrayList<>(list);
            selected = requests.stream().filter(r -> r.getId().equals(selectId)).findFirst()
                    .orElse(requests.isEmpty() ? null : requests.get(0));
            updateCounts();
            renderList();
            renderDetail();
        }, e -> Toast.error(listBox, Async.userMessage(e)));
    }

    private void updateCounts() {
        allFilter.setText("Toutes (" + requests.size() + ")");
        activeFilter.setText("En cours (" + requests.stream().filter(r -> r.getStatus().isActive()).count() + ")");
        doneFilter.setText("Terminées (" + requests.stream().filter(r -> r.getStatus().isFinished()).count() + ")");
    }

    private Predicate<ServiceRequest> currentFilter() {
        if (activeFilter.isSelected()) return r -> r.getStatus().isActive();
        if (doneFilter.isSelected()) return r -> r.getStatus().isFinished();
        return r -> true;
    }

    private void renderList() {
        listBox.getChildren().clear();
        List<ServiceRequest> visible = requests.stream().filter(currentFilter()).toList();
        if (visible.isEmpty()) {
            listBox.getChildren().add(Ui.emptyState("fth-clipboard", "Aucune demande ici",
                    requests.isEmpty() ? "Publiez votre première demande pour recevoir des devis."
                            : "Aucune demande ne correspond à ce filtre.", null));
            return;
        }
        for (ServiceRequest r : visible) {
            listBox.getChildren().add(Cards.requestRow(r, r.equals(selected), () -> {
                selected = r;
                renderList();
                renderDetail();
            }));
        }
    }

    private void renderDetail() {
        detailBox.getChildren().clear();
        actionsBar.getChildren().clear();
        actionsBar.setVisible(false);
        actionsBar.setManaged(false);
        if (selected == null) {
            Button create = Ui.button("Nouvelle demande", "btn-primary", "fth-plus");
            create.setOnAction(e -> onNewRequest());
            detailBox.getChildren().add(Ui.emptyState("fth-clipboard", "Vous n'avez pas encore de demande",
                    "Décrivez votre besoin et recevez des devis d'artisans qualifiés.", create));
            return;
        }
        ServiceRequest r = selected;
        RequestStatus status = r.getStatus();

        // En-tête
        HBox titleRow = new HBox(12, Ui.label(r.getTitle(), "detail-title"), Ui.statusBadge(status));
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label sub = Ui.label("Demande " + r.getReference() + " · " + r.getTrade().getNameFr()
                + " · créée le " + Formats.date(r.getCreatedAt()), "text-muted");
        VBox headText = new VBox(6, titleRow, sub);
        HBox.setHgrow(headText, Priority.ALWAYS);
        HBox header = new HBox(16, headText, ma.fixi.view.component.Images.trade(r.getTrade(), 88));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("card-hero");
        detailBox.getChildren().add(header);

        // Message contextuel
        Node callout = calloutFor(r);
        if (callout != null) {
            detailBox.getChildren().add(callout);
        }

        // Artisan (ou état des devis) + informations clés
        HBox top = new HBox(16, artisanPanel(r), keyFacts(r));
        detailBox.getChildren().add(top);

        // Frise de suivi
        if (status.getTimelineStep() >= 0) {
            VBox follow = new VBox(16, Ui.label("Suivi de la demande", "section-title"), Timeline.of(status));
            detailBox.getChildren().add(follow);
        }

        // Détails
        Label description = new Label(r.getDescription());
        description.setWrapText(true);
        VBox details = new VBox(10, Ui.label("Détails du service", "section-title"), description);
        details.getChildren().add(Ui.iconText("fth-alert-triangle", "Urgence : " + r.getUrgency().getLabel(), "text-muted"));
        if (!r.getAttachmentNames().isEmpty()) {
            details.getChildren().add(Ui.iconText("fth-image", r.getAttachmentNames().size() + " photo(s) jointe(s)",
                    "text-muted"));
        }
        details.getStyleClass().add("card-tint");
        detailBox.getChildren().add(details);

        // Actions (barre fixe en bas)
        HBox actions = actionsBar;
        if (status.canBeCancelledByClient()) {
            Button cancel = Ui.button("Annuler la demande", "btn-danger-outline", "fth-x");
            cancel.setOnAction(e -> Dialogs.confirm(detailBox, "Annuler cette demande ?",
                    "Les artisans ne pourront plus vous envoyer de devis. Cette action est définitive.",
                    "Annuler la demande", true, () -> run(() -> Services.requests().cancel(r.getId()),
                            "Demande annulée.")));
            actions.getChildren().add(cancel);
        }
        if (status.canChangeDate()) {
            Button date = Ui.button("Modifier la date", "btn-secondary", "fth-calendar");
            date.setOnAction(e -> openDateDialog(r));
            actions.getChildren().add(date);
        }
        if (status.canBeValidatedByClient()) {
            Button confirm = Ui.button("Confirmer la fin des travaux", "btn-primary", "fth-check");
            confirm.setOnAction(e -> Dialogs.confirm(detailBox, "Confirmer la fin des travaux ?",
                    "Confirmez seulement si les travaux sont réalisés comme prévu. Vous pourrez ensuite laisser un avis.",
                    "Confirmer", false, () -> run(() -> Services.requests().confirmCompletion(r.getId()),
                            "Travaux confirmés. Merci !")));
            actions.getChildren().add(confirm);
        }
        actionsBar.setVisible(!actions.getChildren().isEmpty());
        actionsBar.setManaged(!actions.getChildren().isEmpty());
    }

    private Node calloutFor(ServiceRequest r) {
        String name = r.getAssignedArtisan() == null ? "L'artisan" : r.getAssignedArtisan().getDisplayName();
        return switch (r.getStatus()) {
            case DONE_BY_ARTISAN -> callout("fth-alert-circle", name
                    + " a déclaré les travaux terminés. Vérifiez le résultat, puis confirmez la fin.", false);
            case CANCELLED -> callout("fth-x-circle", "Cette demande a été annulée.", false);
            case EXPIRED -> callout("fth-clock", "Cette demande a expiré sans devis accepté.", false);
            case DISPUTED -> callout("fth-alert-triangle", "Un litige est ouvert. L'équipe Fixi vous contactera.", false);
            default -> null;
        };
    }

    private HBox callout(String icon, String text, boolean info) {
        Label l = new Label(text);
        l.setWrapText(true);
        HBox box = new HBox(10, Ui.icon(icon, 18), l);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("callout");
        if (info) box.getStyleClass().add("callout-info");
        return box;
    }

    private VBox artisanPanel(ServiceRequest r) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("card-tint");
        HBox.setHgrow(panel, Priority.ALWAYS);
        panel.setPrefWidth(360);
        if (r.getAssignedArtisan() != null) {
            var a = r.getAssignedArtisan();
            Button contact = Ui.button("Contacter", "btn-primary", "fth-message-square");
            contact.getStyleClass().add("btn-small");
            contact.setOnAction(e -> Actions.contactArtisan(detailBox, a.getId()));
            Label name = Ui.label(a.getDisplayName(), "h3");
            name.setOnMouseClicked(e -> Navigator.goTo(Route.ARTISAN_PROFILE, a.getId()));
            name.setStyle("-fx-cursor: hand;");
            VBox text = new VBox(4, name, Ui.rating(a.getAverageRating(), a.getReviewCount()), contact);
            VBox.setMargin(contact, new javafx.geometry.Insets(6, 0, 0, 0));
            HBox row = new HBox(14, Ui.avatar(a.getDisplayName(), 64, a.isVerified()), text);
            row.setAlignment(Pos.CENTER_LEFT);
            panel.getChildren().addAll(Ui.label("Artisan retenu", "overline"), row);
        } else if (r.getStatus() == RequestStatus.QUOTE_RECEIVED) {
            panel.getChildren().addAll(Ui.label("Devis", "overline"),
                    Ui.label(r.getQuoteCount() + " devis reçus", "h3"),
                    wrap("La comparaison côte à côte des devis arrive dans la prochaine version.", "text-muted"));
        } else if (r.getStatus() == RequestStatus.SENT) {
            panel.getChildren().addAll(Ui.label("Devis", "overline"),
                    Ui.label("En attente de devis", "h3"),
                    wrap("Votre demande a été envoyée aux artisans en " + r.getTrade().getNameFr().toLowerCase()
                            + " à " + r.getCity().getNameFr() + ".", "text-muted"));
        } else {
            panel.getChildren().addAll(Ui.label("Artisan", "overline"), wrap("Aucun artisan retenu.", "text-muted"));
        }
        return panel;
    }

    private VBox keyFacts(ServiceRequest r) {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        addFact(grid, 0, "fth-tag", "Budget", r.getBudget() == null ? "Non précisé" : Formats.mad(r.getBudget()));
        addFact(grid, 1, "fth-calendar", "Date souhaitée", Formats.date(r.getPreferredDate()));
        addFact(grid, 2, "fth-map-pin", "Lieu", r.getCity().getNameFr());
        if (r.getAcceptedQuoteTotal() != null) {
            addFact(grid, 3, "fth-file-text", "Devis accepté", Formats.mad(r.getAcceptedQuoteTotal()));
        }
        VBox box = new VBox(grid);
        box.getStyleClass().add("card-tint");
        box.setMinWidth(280);
        return box;
    }

    private void addFact(GridPane grid, int row, String icon, String key, String value) {
        grid.add(Ui.iconText(icon, key, "kv-key"), 0, row);
        grid.add(Ui.label(value, "kv-value"), 1, row);
    }

    private void openDateDialog(ServiceRequest r) {
        DatePicker picker = new DatePicker(r.getPreferredDate());
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(java.time.LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(java.time.LocalDate.now()));
            }
        });
        Label hint = Ui.label("L'artisan sera prévenu du changement.", "text-subtle");
        Button cancel = Ui.button("Retour", "btn-secondary", null);
        Button save = Ui.button("Enregistrer", "btn-primary", null);
        Dialogs.Handle h = Dialogs.show(detailBox, "Modifier la date souhaitée", new VBox(8, picker, hint),
                List.of(cancel, save));
        cancel.setOnAction(e -> h.close());
        save.setOnAction(e -> {
            if (picker.getValue() == null) {
                return;
            }
            h.close();
            run(() -> Services.requests().changePreferredDate(r.getId(), picker.getValue()), "Date modifiée.");
        });
    }

    /** Lance une action sur la demande puis recharge la page en gardant la sélection. */
    private void run(java.util.concurrent.Callable<ServiceRequest> action, String successMessage) {
        Async.run(action, updated -> {
            Toast.success(detailBox, successMessage);
            load(updated.getId());
        }, e -> Toast.error(detailBox, Async.userMessage(e)));
    }

    private static Label wrap(String text, String style) {
        Label l = Ui.label(text, style);
        l.setWrapText(true);
        return l;
    }

    @FXML
    private void onNewRequest() {
        Navigator.goTo(Route.NEW_REQUEST);
    }
}
