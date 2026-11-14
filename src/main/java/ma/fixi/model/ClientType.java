package ma.fixi.model;

/** Type de client. Correspond à la colonne {@code user.client_type}. */
public enum ClientType {
    PARTICULIER("Particulier"),
    SYNDIC("Syndic");

    private final String label;

    ClientType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
