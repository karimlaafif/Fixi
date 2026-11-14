package ma.fixi.view.component;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import ma.fixi.model.Trade;

/**
 * Illustrations de l'application (src/main/resources/images), chargées une seule fois.
 *
 * <ul>
 *   <li>{@code mascot.png} : l'artisan devant la maison (écrans de connexion, bannières) ;</li>
 *   <li>{@code logo-mark.png} : logo Fixi ;</li>
 *   <li>{@code trades/<metier>.png} : une illustration par métier (nom sans accents, en minuscules).</li>
 * </ul>
 *
 * Un métier sans illustration (par exemple ajouté par l'administrateur) retombe sur une icône simple.
 */
public final class Images {

    public static final String MASCOT = "/images/mascot.png";
    public static final String LOGO = "/images/logo-mark.png";
    /** Tracé blanc seul, pour le menu latéral violet. */
    public static final String LOGO_WHITE = "/images/logo-white.png";

    private static final Map<String, Image> CACHE = new HashMap<>();

    private Images() {
    }

    /** Image du dossier resources, ou {@code null} si le fichier n'existe pas. */
    public static Image load(String path) {
        return CACHE.computeIfAbsent(path, p -> {
            try (InputStream in = Images.class.getResourceAsStream(p)) {
                return in == null ? null : new Image(in);
            } catch (java.io.IOException e) {
                return null;
            }
        });
    }

    /** Image redimensionnée proprement (proportions conservées) dans un carré ou un rectangle donné. */
    public static ImageView view(String path, double fitWidth, double fitHeight) {
        ImageView v = new ImageView(load(path));
        v.setPreserveRatio(true);
        v.setSmooth(true);
        if (fitWidth > 0) v.setFitWidth(fitWidth);
        if (fitHeight > 0) v.setFitHeight(fitHeight);
        return v;
    }

    public static ImageView logo(double size) {
        return view(LOGO, size, size);
    }

    /** Illustration du métier dans une case de {@code size} pixels, ou une icône simple à défaut. */
    public static Node trade(Trade trade, double size) {
        Image img = trade == null ? null : load("/images/trades/" + slug(trade.getNameFr()) + ".png");
        if (img == null) {
            return Ui.iconTile(Ui.tradeIcon(trade), size, "tile-lilac");
        }
        ImageView v = new ImageView(img);
        v.setPreserveRatio(true);
        v.setSmooth(true);
        v.setFitWidth(size);
        v.setFitHeight(size);
        StackPane box = new StackPane(v);
        box.setMinSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }

    /** « Électricité » → « electricite ». */
    static String slug(String name) {
        if (name == null) {
            return "";
        }
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-");
    }
}
