package ma.fixi.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ma.fixi.model.Artisan;
import ma.fixi.model.Review;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.util.Formats;
import ma.fixi.view.Navigator;
import ma.fixi.view.Page;
import ma.fixi.view.Route;
import ma.fixi.view.component.Toast;
import ma.fixi.view.component.Ui;

/** Profil public d'un artisan. Appelle {@code ArtisanService.findById}. */
public class ArtisanProfileController implements Page {

    @FXML private StackPane avatarBox;
    @FXML private HBox nameRow;
    @FXML private Label nameLabel;
    @FXML private Label tradeLabel;
    @FXML private HBox ratingBox;
    @FXML private VBox factsBox;
    @FXML private ToggleGroup tabs;
    @FXML private ToggleButton aboutTab;
    @FXML private ToggleButton reviewsTab;
    @FXML private VBox tabContent;
    @FXML private VBox infoBox;
    @FXML private Button requestButton;

    private Artisan artisan;

    @FXML
    private void initialize() {
        tabs.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                tabs.selectToggle(old);
            } else {
                showTab();
            }
        });
    }

    @Override
    public void onShow(Route route, Object param) {
        String artisanId = (String) param;
        Async.run(() -> Services.artisans().findById(artisanId), found -> {
            if (found.isEmpty()) {
                Toast.error(tabContent, "Ce profil n'est plus disponible.");
                Navigator.back();
                return;
            }
            artisan = found.get();
            fill();
        }, e -> Toast.error(tabContent, Async.userMessage(e)));
    }

    private void fill() {
        avatarBox.getChildren().setAll(Ui.avatar(artisan.getDisplayName(), 112, artisan.isVerified()));
        nameLabel.setText(artisan.getDisplayName());
        if (artisan.isVerified()) {
            nameRow.getChildren().add(Ui.verifiedChip());
        }
        tradeLabel.setText(artisan.getTrade().getNameFr());
        tradeLabel.setGraphic(Ui.icon(Ui.tradeIcon(artisan.getTrade()), 17, "icon-primary"));
        ratingBox.getChildren().setAll(Ui.rating(artisan.getAverageRating(), artisan.getReviewCount()));

        factsBox.getChildren().setAll(
                Ui.iconText("fth-map-pin", artisan.getCity().getNameFr()),
                Ui.iconText("fth-briefcase", Formats.years(artisan.getYearsOfExperience())),
                Ui.iconText("fth-tag", "À partir de " + Formats.mad(artisan.getPriceFrom())));
        factsBox.setAlignment(Pos.CENTER_LEFT);

        reviewsTab.setText("Avis (" + artisan.getReviewCount() + ")");

        Label availability = Ui.label(artisan.getAvailability().getLabel(), "availability-chip");
        availability.setGraphic(Ui.icon("fth-circle", 9, "icon-success"));
        availability.setGraphicTextGap(8);
        infoBox.getChildren().setAll(
                infoRow("fth-map", "Zones couvertes", new Label(String.join(", ", artisan.getCoveredAreas()))),
                infoRow("fth-tag", "Tarif indicatif", new Label("À partir de " + Formats.mad(artisan.getPriceFrom())
                        + ", le prix final figure sur le devis.")),
                infoRow("fth-calendar", "Disponibilités", availability));
        showTab();
    }

    private HBox infoRow(String icon, String title, Node value) {
        if (value instanceof Label l) {
            l.setWrapText(true);
        }
        VBox text = new VBox(4, Ui.label(title, "info-row-title"), value);
        HBox row = new HBox(14, Ui.iconTile(icon, 40, "tile-lilac"), text);
        row.getStyleClass().add("card-tint");
        return row;
    }

    private void showTab() {
        if (artisan == null) {
            return;
        }
        tabContent.getChildren().clear();
        if (aboutTab.isSelected()) {
            Label description = new Label(artisan.getDescription());
            description.setWrapText(true);
            description.setStyle("-fx-font-size: 15px; -fx-line-spacing: 3px;");
            tabContent.getChildren().addAll(description,
                    Ui.label("Profil vérifié par Fixi : identité, ICE ou statut d'auto-entrepreneur et assurance.",
                            "text-subtle"));
            if (!artisan.isVerified()) {
                ((Label) tabContent.getChildren().get(1)).setText(
                        "Ce profil n'a pas encore été vérifié par Fixi.");
            }
            return;
        }
        if (artisan.getReviews().isEmpty()) {
            tabContent.getChildren().add(Ui.emptyState("fth-star", "Pas encore d'avis",
                    "Les avis sont publiés par les clients après une intervention clôturée.", null));
            return;
        }
        for (Review r : artisan.getReviews()) {
            HBox stars = new HBox(2);
            for (int i = 1; i <= 5; i++) {
                stars.getChildren().add(Ui.icon("fth-star", 14, i <= r.getRating() ? "icon-star" : "icon-muted"));
            }
            HBox head = new HBox(10, Ui.avatar(r.getAuthorName(), 34, false), Ui.label(r.getAuthorName(), "semibold"),
                    stars, Ui.hSpacer(), Ui.label(Formats.date(r.getPublishedOn()), "text-subtle"));
            head.setAlignment(Pos.CENTER_LEFT);
            Label comment = new Label(r.getComment());
            comment.setWrapText(true);
            VBox card = new VBox(8, head, comment);
            card.getStyleClass().add("review-card");
            tabContent.getChildren().add(card);
        }
        tabContent.getChildren().add(Ui.label("Seuls les clients ayant terminé une intervention peuvent laisser un avis.",
                "text-subtle"));
    }

    @FXML
    private void onBack() {
        Navigator.back();
    }

    @FXML
    private void onRequest() {
        if (artisan != null) {
            Navigator.goTo(Route.NEW_REQUEST, artisan);
        }
    }

    @FXML
    private void onContact() {
        if (artisan != null) {
            Actions.contactArtisan(tabContent, artisan.getId());
        }
    }
}
