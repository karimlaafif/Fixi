package ma.fixi.model;

/** Rôles d'un compte. Correspond à la colonne {@code user_role.role_code}. */
public enum Role {
    CLIENT("Client"),
    ARTISAN("Artisan"),
    ADMINISTRATOR("Administrateur"),
    SUPPORT("Support");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
