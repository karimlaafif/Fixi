package ma.fixi.view;

/**
 * Contrôleur d'une page de l'espace connecté.
 * {@link #onShow} est appelé chaque fois que la page est affichée, avec un paramètre facultatif
 * (par exemple l'id de l'artisan pour la page de profil).
 */
public interface Page {

    void onShow(Route route, Object param);
}
