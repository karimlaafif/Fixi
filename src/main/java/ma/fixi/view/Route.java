package ma.fixi.view;

import java.util.List;
import ma.fixi.model.Role;

/**
 * Pages de l'espace connecté. Chaque route indique son fichier FXML, son titre et son icône de menu.
 * Les pages « placeholder » sont prévues pour une itération suivante.
 */
public enum Route {
    // Espace client
    HOME("home.fxml", "Accueil", null, "fth-home"),
    SEARCH("search.fxml", "Rechercher des artisans", "Trouvez le professionnel adapté à vos travaux", "fth-search"),
    ARTISAN_PROFILE("artisan-profile.fxml", "Profil de l'artisan", null, null),
    REQUESTS("requests.fxml", "Mes demandes", "Suivez et gérez vos demandes de service", "fth-clipboard"),
    NEW_REQUEST("new-request.fxml", "Nouvelle demande", "Décrivez votre besoin, les artisans vous envoient leurs devis", null),

    // Espace artisan
    ARTISAN_DASHBOARD("artisan-dashboard.fxml", "Tableau de bord", "Votre espace artisan", "fth-grid"),
    ARTISAN_REQUESTS("artisan-requests.fxml", "Demandes reçues", "Les demandes de clients qui correspondent à votre métier", "fth-inbox"),
    AGENDA("placeholder.fxml", "Mon agenda", "Vos rendez-vous et disponibilités", "fth-calendar"),

    // Commun
    MESSAGES("messages.fxml", "Messages", "Échangez avec vos interlocuteurs autour de chaque demande", "fth-message-square"),
    PROFILE("profile.fxml", "Mon profil", "Vos informations personnelles", "fth-user"),

    // Administration
    ADMIN_TRADES("admin-trades.fxml", "Métiers", "Ajoutez, modifiez ou désactivez les métiers proposés aux clients", "fth-tool"),
    ADMIN_CITIES("placeholder.fxml", "Villes", "Villes couvertes par la plateforme", "fth-map-pin"),
    ADMIN_ARTISANS("placeholder.fxml", "Validation des artisans", "Vérification des justificatifs", "fth-user-check");

    private final String fxml;
    private final String title;
    private final String subtitle;
    private final String icon;

    Route(String fxml, String title, String subtitle, String icon) {
        this.fxml = fxml;
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
    }

    /** Entrées du menu latéral, selon le rôle. */
    public static List<Route> menuFor(Role role) {
        return switch (role) {
            case ARTISAN -> List.of(ARTISAN_DASHBOARD, ARTISAN_REQUESTS, AGENDA, MESSAGES, PROFILE);
            case ADMINISTRATOR, SUPPORT -> List.of(ADMIN_TRADES, ADMIN_CITIES, ADMIN_ARTISANS, PROFILE);
            case CLIENT -> List.of(HOME, SEARCH, REQUESTS, MESSAGES, PROFILE);
        };
    }

    /** Page d'accueil après connexion, selon le rôle. */
    public static Route homeFor(Role role) {
        return menuFor(role).get(0);
    }

    /** Entrée du menu à mettre en surbrillance quand cette page est affichée. */
    public Route menuEntry() {
        return switch (this) {
            case ARTISAN_PROFILE -> SEARCH;
            case NEW_REQUEST -> REQUESTS;
            default -> this;
        };
    }

    public String getFxml() {
        return fxml;
    }

    public String getTitle() {
        return title;
    }

    /** Libellé court pour le menu latéral. */
    public String getMenuLabel() {
        return switch (this) {
            case SEARCH -> "Rechercher";
            case ADMIN_ARTISANS -> "Validation";
            default -> title;
        };
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getIcon() {
        return icon;
    }
}
