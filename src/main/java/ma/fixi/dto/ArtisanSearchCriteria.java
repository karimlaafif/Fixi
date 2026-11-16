package ma.fixi.dto;

import ma.fixi.model.Availability;
import ma.fixi.model.City;
import ma.fixi.model.Trade;

/**
 * Filtres de l'écran « Rechercher des artisans ». Chaque champ {@code null} signifie « pas de filtre ».
 *
 * @param text          texte libre comparé au nom de l'artisan et au métier
 * @param minRating     note minimale (0 = toutes)
 * @param availableBy   disponibilité au plus tard à cette échéance
 * @param verifiedOnly  ne garder que les profils vérifiés
 */
public record ArtisanSearchCriteria(
        String text,
        Trade trade,
        City city,
        double minRating,
        Availability availableBy,
        boolean verifiedOnly,
        Sort sort) {

    /** Ordre de tri proposé à l'utilisateur. */
    public enum Sort {
        RATING("Note (la plus élevée)"),
        PRICE("Prix (le moins cher)"),
        EXPERIENCE("Expérience");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Critères vides : tous les artisans, triés par note. */
    public static ArtisanSearchCriteria all() {
        return new ArtisanSearchCriteria(null, null, null, 0, null, false, Sort.RATING);
    }
}
