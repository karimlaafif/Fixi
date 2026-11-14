package ma.fixi.view.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ma.fixi.model.Artisan;
import ma.fixi.model.Availability;
import ma.fixi.model.ServiceRequest;
import ma.fixi.util.Formats;

/** Cartes répétées dans les listes (demandes, artisans). */
public final class Cards {

    private Cards() {
    }

    /** Ligne d'une demande : icône du métier, titre, date, statut. Toute la ligne est cliquable. */
    public static HBox requestRow(ServiceRequest r, boolean selected, Runnable onOpen) {
        Label title = Ui.label(r.getTitle(), "request-title");
        title.setWrapText(true);
        Label meta = Ui.label(r.getReference() + "  ·  " + r.getCity().getNameFr() + "  ·  "
                + Formats.relativeDay(r.getCreatedAt().toLocalDate()), "text-subtle");
        VBox text = new VBox(3, title, meta);
        HBox.setHgrow(text, Priority.ALWAYS);
        text.setMinWidth(0);

        HBox row = new HBox(14, Images.trade(r.getTrade(), 44), text,
                Ui.statusBadge(r.getStatus()), Ui.icon("fth-chevron-right", 18, "icon-muted"));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("request-row");
        if (selected) {
            row.getStyleClass().add("selected");
        }
        row.setOnMouseClicked(e -> onOpen.run());
        return row;
    }

    /** Carte compacte d'un artisan (accueil client). */
    public static HBox artisanCompact(Artisan a, Runnable onOpen) {
        Label name = Ui.label(a.getDisplayName(), "semibold");
        Label trade = Ui.label(a.getTrade().getNameFr() + " · " + a.getCity().getNameFr(), "text-subtle");
        VBox text = new VBox(2, name, trade, Ui.rating(a.getAverageRating(), a.getReviewCount()));
        HBox.setHgrow(text, Priority.ALWAYS);
        Button open = Ui.button("Voir", "btn-secondary", null);
        open.getStyleClass().add("btn-small");
        open.setOnAction(e -> onOpen.run());
        HBox row = new HBox(12, Ui.avatar(a.getDisplayName(), 48, a.isVerified()), text, open);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("card-tint", "card-clickable");
        row.setOnMouseClicked(e -> onOpen.run());
        return row;
    }

    /** Résultat de recherche : identité, informations clés et actions. */
    public static HBox artisanResult(Artisan a, Runnable onProfile, Runnable onContact) {
        Label name = Ui.label(a.getDisplayName(), "artisan-name");
        name.setWrapText(true);
        // Le badge « Vérifié » est sur la ligne du métier pour laisser toute la largeur au nom.
        HBox tradeRow = new HBox(10, Ui.label(a.getTrade().getNameFr(), "trade-label"));
        tradeRow.setAlignment(Pos.CENTER_LEFT);
        if (a.isVerified()) {
            tradeRow.getChildren().add(Ui.verifiedChip());
        }
        VBox identity = new VBox(4, name, tradeRow, Ui.iconText("fth-map-pin", a.getCity().getNameFr()),
                Ui.rating(a.getAverageRating(), a.getReviewCount()));
        identity.setMinWidth(0);
        identity.setPrefWidth(240);
        HBox.setHgrow(identity, Priority.ALWAYS);

        Label availability = Ui.iconText("fth-calendar", a.getAvailability().getLabel());
        if (a.getAvailability() == Availability.TODAY) {
            availability.getStyleClass().add("text-primary");
        }
        VBox facts = new VBox(8, availability,
                Ui.iconText("fth-briefcase", Formats.years(a.getYearsOfExperience())),
                Ui.iconText("fth-tag", "À partir de " + Formats.mad(a.getPriceFrom())));
        facts.setMinWidth(205);
        facts.setPrefWidth(205);
        facts.setAlignment(Pos.CENTER_LEFT);

        Button contact = Ui.button("Contacter", "btn-secondary", "fth-message-square");
        contact.setMaxWidth(Double.MAX_VALUE);
        contact.setOnAction(e -> onContact.run());
        Button profile = Ui.button("Voir le profil", "btn-primary", null);
        profile.setMaxWidth(Double.MAX_VALUE);
        profile.setOnAction(e -> onProfile.run());
        VBox actions = new VBox(8, profile, contact);
        actions.setMinWidth(140);
        actions.setMaxWidth(140);
        actions.setAlignment(Pos.CENTER);

        Region sep1 = new Region();
        sep1.getStyleClass().add("divider-vertical");
        Region sep2 = new Region();
        sep2.getStyleClass().add("divider-vertical");

        HBox card = new HBox(18, Ui.avatar(a.getDisplayName(), 64, a.isVerified()), identity, sep1, facts, sep2, actions);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("card");
        return card;
    }
}
