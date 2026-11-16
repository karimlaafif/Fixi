package ma.fixi.model;

/**
 * Disponibilité résumée d'un artisan, calculée à partir de {@code availability_slot}.
 * L'ordre des constantes va de la plus proche à la plus lointaine.
 */
public enum Availability {
    TODAY("Disponible aujourd'hui"),
    TOMORROW("Disponible demain"),
    THIS_WEEK("Disponible cette semaine"),
    LATER("Disponible plus tard");

    private final String label;

    Availability(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Vrai si l'artisan est disponible au plus tard à l'échéance donnée. */
    public boolean isAtLeast(Availability limit) {
        return ordinal() <= limit.ordinal();
    }
}
