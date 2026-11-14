package ma.fixi.view.component;

import java.util.Locale;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import ma.fixi.model.RequestStatus;
import ma.fixi.model.Trade;
import ma.fixi.util.Formats;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Petits composants réutilisés par plusieurs écrans (avatar, badge, note, icône de métier…).
 * Le style vient de {@code css/fixi.css} : ces méthodes ne posent que des classes CSS.
 */
public final class Ui {

    private static final String[] AVATAR_TONES = {"avatar-tone-1", "avatar-tone-2", "avatar-tone-3", "avatar-tone-4", "avatar-tone-5"};

    private Ui() {
    }

    public static FontIcon icon(String literal, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        icon.getStyleClass().add("icon");
        return icon;
    }

    public static FontIcon icon(String literal, int size, String styleClass) {
        FontIcon icon = icon(literal, size);
        icon.getStyleClass().add(styleClass);
        return icon;
    }

    public static Label label(String text, String... styleClasses) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styleClasses);
        return l;
    }

    /** Icône + texte sur une ligne (« 📍 Rabat »). */
    public static Label iconText(String iconLiteral, String text, String... styleClasses) {
        Label l = new Label(text, icon(iconLiteral, 15, "icon-muted"));
        l.setGraphicTextGap(8);
        l.getStyleClass().addAll(styleClasses);
        return l;
    }

    /** Pastille ronde avec les initiales ; coche verte si le profil est vérifié. */
    public static StackPane avatar(String name, double size, boolean verified) {
        Circle circle = new Circle(size / 2);
        circle.getStyleClass().addAll("avatar-circle", toneFor(name));
        Label initials = new Label(initials(name));
        initials.getStyleClass().add("avatar-initials");
        initials.setStyle("-fx-font-size: " + Math.round(size * 0.36) + "px;");
        StackPane avatar = new StackPane(circle, initials);
        avatar.setMinSize(size, size);
        avatar.setMaxSize(size, size);
        if (verified) {
            double badge = Math.max(16, size * 0.3);
            StackPane check = new StackPane(icon("fth-check", (int) (badge * 0.6), "icon-on-success"));
            check.getStyleClass().add("avatar-verified");
            check.setMinSize(badge, badge);
            check.setMaxSize(badge, badge);
            StackPane.setAlignment(check, Pos.BOTTOM_RIGHT);
            avatar.getChildren().add(check);
        }
        return avatar;
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }

    private static String toneFor(String name) {
        return AVATAR_TONES[Math.floorMod(name == null ? 0 : name.hashCode(), AVATAR_TONES.length)];
    }

    /** Badge coloré du statut d'une demande. */
    public static Label statusBadge(RequestStatus status) {
        return badge(status.getLabel(), "badge-" + status.getTone().name().toLowerCase(Locale.ROOT));
    }

    public static Label badge(String text, String toneClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll("badge", toneClass);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    /** Pastille « ✓ Vérifié ». */
    public static Label verifiedChip() {
        Label l = new Label("Vérifié", icon("fth-check", 12, "icon-success"));
        l.setGraphicTextGap(4);
        l.getStyleClass().addAll("badge", "badge-success");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    /** « ★ 4,8 (120 avis) ». */
    public static HBox rating(double value, int count) {
        Label score = label(Formats.rating(value), "rating-value");
        Label reviews = label("(" + Formats.reviews(count) + ")", "text-muted");
        HBox box = new HBox(5, icon("fth-star", 15, "icon-star"), score, reviews);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** Carré coloré contenant une icône (tuiles de métiers, étapes, statistiques). */
    public static StackPane iconTile(String iconLiteral, double size, String toneClass) {
        StackPane tile = new StackPane(icon(iconLiteral, (int) (size * 0.45)));
        tile.getStyleClass().addAll("icon-tile", toneClass);
        tile.setMinSize(size, size);
        tile.setMaxSize(size, size);
        return tile;
    }

    /**
     * Icône Feather associée à un métier, devinée à partir de son nom.
     * Une future colonne {@code trade.icon} pourra remplacer cette correspondance.
     */
    public static String tradeIcon(Trade trade) {
        if (trade == null || trade.getNameFr() == null) {
            return "fth-tool";
        }
        String n = trade.getNameFr().toLowerCase(Locale.ROOT);
        if (n.contains("plomb")) return "fth-droplet";
        if (n.contains("lectri")) return "fth-zap";
        if (n.contains("peint")) return "fth-edit-3";
        if (n.contains("menuis")) return "fth-box";
        if (n.contains("inform")) return "fth-monitor";
        if (n.contains("clim")) return "fth-wind";
        if (n.contains("serru")) return "fth-key";
        if (n.contains("jardin")) return "fth-sun";
        if (n.contains("nettoy") || n.contains("ménage")) return "fth-trash-2";
        return "fth-tool";
    }

    /** Message affiché quand une liste est vide, avec une action facultative. */
    public static VBox emptyState(String iconLiteral, String title, String message, Button action) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("empty-state");
        StackPane tile = iconTile(iconLiteral, 52, "tile-lilac");
        Label t = label(title, "empty-title");
        Label m = label(message, "text-muted");
        m.setWrapText(true);
        m.setMaxWidth(360);
        m.setStyle("-fx-text-alignment: center;");
        box.getChildren().addAll(tile, t, m);
        if (action != null) {
            VBox.setMargin(action, new javafx.geometry.Insets(6, 0, 0, 0));
            box.getChildren().add(action);
        }
        return box;
    }

    public static Button button(String text, String styleClass, String iconLiteral) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        if (iconLiteral != null) {
            b.setGraphic(icon(iconLiteral, 15));
            b.setGraphicTextGap(8);
        }
        b.setMinWidth(Region.USE_PREF_SIZE);
        return b;
    }

    public static Region hSpacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Region vSpacer() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Ajoute une classe CSS une seule fois. */
    public static void addClass(Node node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }
}
