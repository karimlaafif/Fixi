package ma.fixi.model;

/** Niveau d'urgence d'une demande ({@code service_request.urgency}). */
public enum Urgency {
    NORMAL("Normale"),
    URGENT("Urgente");

    private final String label;

    Urgency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
