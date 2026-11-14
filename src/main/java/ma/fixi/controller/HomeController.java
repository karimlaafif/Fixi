package ma.fixi.controller;

import java.util.List;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ma.fixi.model.City;
import ma.fixi.model.ServiceRequest;
import ma.fixi.model.Trade;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.view.Navigator;
import ma.fixi.view.Page;
import ma.fixi.view.Route;
import ma.fixi.view.Session;
import ma.fixi.view.component.Cards;
import ma.fixi.view.component.Toast;
import ma.fixi.view.component.Ui;

/** Accueil client : recherche rapide, métiers, demandes en cours, artisans les mieux notés. */
public class HomeController implements Page {

    @FXML private ComboBox<Trade> tradeCombo;
    @FXML private ComboBox<City> cityCombo;
    @FXML private HBox tradesPane;
    @FXML private VBox requestsBox;
    @FXML private VBox artisansBox;

    @Override
    public void onShow(Route route, Object param) {
        Navigator.setHeader("Bonjour, " + Session.user().getFirstName() + " !",
                "Trouvez rapidement un professionnel de confiance.");
        String clientId = Session.user().getId();

        Async.run(() -> Services.trades().findActive(), this::showTrades, this::onError);
        Async.run(() -> Services.cities().findActive(), cities -> cityCombo.getItems().setAll(cities), this::onError);
        Async.run(() -> Services.requests().findByClient(clientId), this::showRequests, this::onError);
        Async.run(() -> Services.artisans().findTopRated(3), artisans -> {
            artisansBox.getChildren().clear();
            artisans.forEach(a -> artisansBox.getChildren().add(
                    Cards.artisanCompact(a, () -> Navigator.goTo(Route.ARTISAN_PROFILE, a.getId()))));
        }, this::onError);
    }

    private void showTrades(List<Trade> trades) {
        tradeCombo.getItems().setAll(trades);
        tradesPane.getChildren().clear();
        // Une seule ligne de tuiles de même largeur ; la liste complète reste accessible par la recherche.
        for (Trade t : trades.stream().limit(8).toList()) {
            VBox tile = new VBox(10, ma.fixi.view.component.Images.trade(t, 58), new Label(t.getNameFr()));
            tile.setAlignment(Pos.CENTER);
            tile.getStyleClass().add("service-tile");
            HBox.setHgrow(tile, Priority.ALWAYS);
            tile.setPrefWidth(0);
            tile.setMaxWidth(Double.MAX_VALUE);
            tile.setOnMouseClicked(e -> Navigator.goTo(Route.SEARCH, new SearchController.Query(null, t, null)));
            tradesPane.getChildren().add(tile);
        }
    }

    private void showRequests(List<ServiceRequest> requests) {
        requestsBox.getChildren().clear();
        List<ServiceRequest> active = requests.stream().filter(r -> r.getStatus().isActive()).limit(3).toList();
        if (active.isEmpty()) {
            Button create = Ui.button("Publier une demande", "btn-primary", "fth-plus");
            create.setOnAction(e -> onNewRequest());
            requestsBox.getChildren().add(Ui.emptyState("fth-clipboard", "Aucune demande en cours",
                    "Décrivez votre besoin : les artisans de votre ville vous enverront leurs devis.", create));
            return;
        }
        for (ServiceRequest r : active) {
            requestsBox.getChildren().add(Cards.requestRow(r, false, () -> Navigator.goTo(Route.REQUESTS, r.getId())));
        }
    }

    @FXML
    private void onSearch() {
        Navigator.goTo(Route.SEARCH, new SearchController.Query(null, tradeCombo.getValue(), cityCombo.getValue()));
    }

    @FXML
    private void onNewRequest() {
        Navigator.goTo(Route.NEW_REQUEST);
    }

    @FXML
    private void onAllArtisans() {
        Navigator.goTo(Route.SEARCH);
    }

    @FXML
    private void onAllRequests() {
        Navigator.goTo(Route.REQUESTS);
    }

    private void onError(Throwable e) {
        Toast.error(requestsBox, Async.userMessage(e));
    }
}
