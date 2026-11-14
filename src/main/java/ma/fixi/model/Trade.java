package ma.fixi.model;

import java.util.Objects;

/** Métier (plombier, électricien…). Correspond à la table {@code trade}. */
public class Trade {

    private String id;
    private String nameFr;
    private String nameAr;
    private boolean active = true;

    public Trade() {
    }

    public Trade(String id, String nameFr, String nameAr, boolean active) {
        this.id = id;
        this.nameFr = nameFr;
        this.nameAr = nameAr;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNameFr() {
        return nameFr;
    }

    public void setNameFr(String nameFr) {
        this.nameFr = nameFr;
    }

    public String getNameAr() {
        return nameAr;
    }

    public void setNameAr(String nameAr) {
        this.nameAr = nameAr;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Texte affiché dans les listes déroulantes. */
    @Override
    public String toString() {
        return nameFr;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Trade other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
