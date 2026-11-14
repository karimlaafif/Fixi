package ma.fixi.controller;

import java.util.List;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ma.fixi.model.Appointment;
import ma.fixi.model.ArtisanStats;
import ma.fixi.model.ServiceRequest;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.util.Formats;
import ma.fixi.view.Navigator;
import ma.fixi.view.Page;
import ma.fixi.view.Route;
import ma.fixi.view.Session;
import ma.fixi.view.component.Toast;
import ma.fixi.view.component.Ui;

/** Tableau de bord artisan : profil, chiffres clés, demandes reçues, prochains rendez-vous. */
public class ArtisanDashboardController implements Page {

    @FXML private StackPane avatarBox;
    @FXML private Label bannerText;
    @FXML private HBox nameRow;
    @FXML private Label nameLabel;
    @FXML private Label tradeLabel;
    @FXML private HBox ratingBox;
    @FXML private HBox statsBox;
    @FXML private VBox requestsBox;
    @FXML private VBox appointmentsBox;

    private String artisanId;

    @Override
    public void onShow(Route route, Object param) {
        artisanId = Session.user().getId();
        Navigator.setHeader("Bonjour, " + Session.user().getFirstName() + " !", "Votre espace artisan");
        nameLabel.setText(Session.user().getFullName());
        bannerText.setText("Gérez vos demandes, vos interventions et vos clients au même endroit.");
        avatarBox.getChildren().setAll(Ui.avatar(Session.user().getFullName(), 64, false));

        Async.run(() -> Services.artisans().findById(artisanId), found -> found.ifPresent(a -> {
            avatarBox.getChildren().setAll(Ui.avatar(a.getDisplayName(), 64, a.isVerified()));
            if (a.isVerified()) {
                nameRow.getChildren().setAll(Ui.verifiedChip());
            }
            tradeLabel.setText(a.getTrade().getNameFr() + " · " + a.getCity().getNameFr());
            ratingBox.getChildren().setAll(Ui.rating(a.getAverageRating(), a.getReviewCount()));
        }), this::onError);
        Async.run(() -> Services.artisans().getStats(artisanId), this::showStats, this::onError);
        loadRequests();
        Async.run(() -> Services.artisans().findUpcomingAppointments(artisanId, 4), this::showAppointments, this::onError);
    }

    private void loadRequests() {
        Async.run(() -> Services.requests().findIncomingForArtisan(artisanId), this::showRequests, this::onError);
    }

    private void showStats(ArtisanStats s) {
        statsBox.getChildren().setAll(
                stat("fth-inbox", "tile-lilac", String.valueOf(s.getNewRequests()), "Nouvelles demandes"),
                stat("fth-calendar", "tile-blue", String.valueOf(s.getPlannedInterventions()), "Interventions prévues"),
                stat("fth-check-circle", "tile-green", String.valueOf(s.getCompletedInterventions()), "Interventions terminées"));
    }

    private VBox stat(String icon, String tone, String value, String label) {
        Label l = Ui.label(label, "text-muted");
        l.setWrapText(true);
        VBox box = new VBox(12, Ui.iconTile(icon, 44, tone), Ui.label(value, "stat-value"), l);
        box.getStyleClass().add("card");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPrefWidth(0);
        box.setMinWidth(140);
        return box;
    }

    private void showRequests(List<ServiceRequest> list) {
        requestsBox.getChildren().clear();
        if (list.isEmpty()) {
            requestsBox.getChildren().add(Ui.emptyState("fth-inbox", "Aucune nouvelle demande",
                    "Les demandes des clients de vos zones apparaîtront ici.", null));
            return;
        }
        list.stream().limit(3).forEach(r ->
                requestsBox.getChildren().add(IncomingRequests.row(r, requestsBox, this::onShowAgain)));
    }

    private void onShowAgain() {
        loadRequests();
        Async.run(() -> Services.artisans().getStats(artisanId), this::showStats, this::onError);
    }

    private void showAppointments(List<Appointment> list) {
        appointmentsBox.getChildren().clear();
        if (list.isEmpty()) {
            appointmentsBox.getChildren().add(Ui.emptyState("fth-calendar", "Aucun rendez-vous",
                    "Vos interventions planifiées apparaîtront ici.", null));
            return;
        }
        for (Appointment a : list) {
            VBox date = new VBox(0, Ui.label(String.valueOf(a.getStartsAt().getDayOfMonth()), "date-tile-day"),
                    Ui.label(Formats.dayMonth(a.getStartsAt().toLocalDate()).replaceAll("^\\d+\\s*", ""), "date-tile-month"));
            date.getStyleClass().add("date-tile");
            VBox text = new VBox(3, Ui.label(a.getTitle(), "semibold"),
                    Ui.iconText("fth-map-pin", a.getCity().getNameFr() + " · " + a.getClientName(), "text-small"));
            HBox.setHgrow(text, Priority.ALWAYS);
            text.setMinWidth(0);
            Label time = new Label(Formats.time(a.getStartsAt()), Ui.icon("fth-clock", 14, "icon-primary"));
            time.getStyleClass().add("time-chip");
            time.setMinWidth(Label.USE_PREF_SIZE);
            HBox row = new HBox(14, date, text, time);
            row.setAlignment(Pos.CENTER_LEFT);
            appointmentsBox.getChildren().add(row);
        }
    }

    @FXML
    private void onAllRequests() {
        Navigator.goTo(Route.ARTISAN_REQUESTS);
    }

    @FXML
    private void onPublicProfile() {
        Navigator.goTo(Route.ARTISAN_PROFILE, artisanId);
    }

    @FXML
    private void onEditProfile() {
        Navigator.goTo(Route.PROFILE);
    }

    private void onError(Throwable e) {
        Toast.error(requestsBox, Async.userMessage(e));
    }
}
