package ma.fixi.controller;

import javafx.scene.Node;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.view.Navigator;
import ma.fixi.view.Route;
import ma.fixi.view.Session;
import ma.fixi.view.component.Toast;

/** Actions partagées par plusieurs écrans. */
final class Actions {

    private Actions() {
    }

    /** Bouton « Contacter » : ouvre (ou crée) la conversation avec l'artisan, puis affiche la messagerie. */
    static void contactArtisan(Node source, String artisanId) {
        String clientId = Session.user().getId();
        Async.run(() -> Services.messages().openWithArtisan(clientId, artisanId),
                conversation -> Navigator.goTo(Route.MESSAGES, conversation.getId()),
                e -> Toast.error(source, Async.userMessage(e)));
    }
}
