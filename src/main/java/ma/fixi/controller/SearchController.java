package ma.fixi.controller;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import ma.fixi.dto.ArtisanSearchCriteria;
import ma.fixi.model.Artisan;
import ma.fixi.model.Availability;
import ma.fixi.model.City;
import ma.fixi.model.Trade;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.util.Formats;
import ma.fixi.view.Navigator;
import ma.fixi.view.Page;
import ma.fixi.view.Route;
import ma.fixi.view.component.Cards;
import ma.fixi.view.component.Combos;
import ma.fixi.view.component.Toast;
import ma.fixi.view.component.Ui;

/** Recherche d'artisans avec filtres appliqués en direct. Appelle {@code ArtisanService.search}. */
public class SearchController implements Page {

    /** Paramètre de navigation : recherche pré-remplie depuis l'accueil ou la barre du haut. */
    public record Query(String text, Trade trade, City city) {
    }

    @FXML private ComboBox<Trade> tradeCombo;
    @FXML private ComboBox<City> cityCombo;
    @FXML private ComboBox<Availability> availabilityCombo;
    @FXML private ComboBox<ArtisanSearchCriteria.Sort> sortCombo;
    @FXML private CheckBox verifiedCheck;
    @FXML private HBox starsBox;
    @FXML private Label ratingHint;
    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private VBox resultsBox;

    private final List<Button> stars = new ArrayList<>();
    private int minRating;
    private boolean loading = true;
    private final PauseTransition typingDelay = new PauseTransition(Duration.millis(250));

    @FXML
    private void initialize() {
        Combos.labels(tradeCombo, Trade::getNameFr, "Tous les métiers");
        Combos.labels(cityCombo, City::getNameFr, "Toutes les villes");
        Combos.labels(availabilityCombo, a -> switch (a) {
            case TODAY -> "Aujourd'hui";
            case TOMORROW -> "D'ici demain";
            case THIS_WEEK -> "Cette semaine";
            case LATER -> "Plus tard";
        }, "Peu importe");
        availabilityCombo.getItems().setAll(null, Availability.TODAY, Availability.TOMORROW, Availability.THIS_WEEK);
        sortCombo.getItems().setAll(ArtisanSearchCriteria.Sort.values());
        sortCombo.setValue(ArtisanSearchCriteria.Sort.RATING);

        for (int i = 1; i <= 5; i++) {
            int value = i;
            Button star = new Button(null, Ui.icon("fth-star", 22));
            star.getStyleClass().add("star-button");
            star.setTooltip(new Tooltip(value + " étoile" + (value > 1 ? "s" : "") + " et plus"));
            star.setOnAction(e -> setMinRating(minRating == value ? 0 : value));
            stars.add(star);
            starsBox.getChildren().add(i - 1, star);
        }
        setMinRating(0);

        tradeCombo.valueProperty().addListener((o, a, b) -> refresh());
        cityCombo.valueProperty().addListener((o, a, b) -> refresh());
        availabilityCombo.valueProperty().addListener((o, a, b) -> refresh());
        sortCombo.valueProperty().addListener((o, a, b) -> refresh());
        verifiedCheck.selectedProperty().addListener((o, a, b) -> refresh());
        typingDelay.setOnFinished(e -> refresh());
        searchField.textProperty().addListener((o, a, b) -> typingDelay.playFromStart());
    }

    @Override
    public void onShow(Route route, Object param) {
        Async.run(() -> new Object[]{Services.trades().findActive(), Services.cities().findActive()}, lists -> {
            @SuppressWarnings("unchecked") List<Trade> trades = (List<Trade>) lists[0];
            @SuppressWarnings("unchecked") List<City> cities = (List<City>) lists[1];
            List<Trade> tradeItems = new ArrayList<>();
            tradeItems.add(null);
            tradeItems.addAll(trades);
            tradeCombo.getItems().setAll(tradeItems);
            List<City> cityItems = new ArrayList<>();
            cityItems.add(null);
            cityItems.addAll(cities);
            cityCombo.getItems().setAll(cityItems);

            if (param instanceof Query q) {
                searchField.setText(q.text() == null ? "" : q.text());
                tradeCombo.setValue(q.trade());
                cityCombo.setValue(q.city());
            } else if (param instanceof String text) {
                searchField.setText(text);
            }
            loading = false;
            refresh();
        }, e -> Toast.error(resultsBox, Async.userMessage(e)));
    }

    private void refresh() {
        if (loading) {
            return;
        }
        ArtisanSearchCriteria criteria = new ArtisanSearchCriteria(searchField.getText(), tradeCombo.getValue(),
                cityCombo.getValue(), minRating, availabilityCombo.getValue(), verifiedCheck.isSelected(),
                sortCombo.getValue());
        Async.run(() -> Services.artisans().search(criteria), this::showResults,
                e -> Toast.error(resultsBox, Async.userMessage(e)));
    }

    private void showResults(List<Artisan> artisans) {
        countLabel.setText(artisans.size() + (artisans.size() > 1 ? " artisans trouvés" : " artisan trouvé"));
        resultsBox.getChildren().clear();
        if (artisans.isEmpty()) {
            Button reset = Ui.button("Effacer les filtres", "btn-secondary", "fth-x");
            reset.setOnAction(e -> onReset());
            VBox empty = Ui.emptyState("fth-search", "Aucun artisan ne correspond",
                    "Essayez une autre ville, retirez un filtre ou publiez une demande : "
                            + "les artisans des environs pourront vous répondre.", reset);
            empty.getStyleClass().add("card");
            resultsBox.getChildren().add(empty);
            return;
        }
        for (Artisan a : artisans) {
            resultsBox.getChildren().add(Cards.artisanResult(a,
                    () -> Navigator.goTo(Route.ARTISAN_PROFILE, a.getId()),
                    () -> Actions.contactArtisan(resultsBox, a.getId())));
        }
    }

    private void setMinRating(int value) {
        minRating = value;
        for (int i = 0; i < stars.size(); i++) {
            stars.get(i).getStyleClass().remove("on");
            if (i < value) {
                stars.get(i).getStyleClass().add("on");
            }
        }
        ratingHint.setText(value == 0 ? "Toutes" : Formats.rating(value) + " et +");
        refresh();
    }

    @FXML
    private void onReset() {
        loading = true;
        searchField.clear();
        tradeCombo.setValue(null);
        cityCombo.setValue(null);
        availabilityCombo.setValue(null);
        verifiedCheck.setSelected(false);
        sortCombo.setValue(ArtisanSearchCriteria.Sort.RATING);
        loading = false;
        setMinRating(0);
    }
}
