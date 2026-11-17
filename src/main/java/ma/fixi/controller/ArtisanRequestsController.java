package ma.fixi.controller;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import ma.fixi.model.ServiceRequest;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.view.Page;
import ma.fixi.view.Route;
import ma.fixi.view.Session;
import ma.fixi.view.component.Toast;
import ma.fixi.view.component.Ui;

/** Liste complète des demandes reçues par l'artisan. */
public class ArtisanRequestsController implements Page {

    @FXML private VBox listBox;

    @Override
    public void onShow(Route route, Object param) {
        load();
    }

    private void load() {
        String artisanId = Session.user().getId();
        Async.run(() -> Services.requests().findIncomingForArtisan(artisanId), this::show,
                e -> Toast.error(listBox, Async.userMessage(e)));
    }

    private void show(List<ServiceRequest> list) {
        listBox.getChildren().clear();
        if (list.isEmpty()) {
            listBox.getChildren().add(Ui.emptyState("fth-inbox", "Aucune demande en attente",
                    "Les nouvelles demandes de clients de vos zones et de votre métier apparaîtront ici.", null));
            return;
        }
        listBox.getChildren().add(Ui.label(list.size() + (list.size() > 1 ? " demandes en attente de réponse"
                : " demande en attente de réponse"), "section-title"));
        for (ServiceRequest r : list) {
            listBox.getChildren().add(IncomingRequests.row(r, listBox, this::load));
        }
    }
}
