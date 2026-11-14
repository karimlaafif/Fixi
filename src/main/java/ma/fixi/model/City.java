package ma.fixi.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Ville. Correspond à la table {@code city}. */
public class City {

    private String id;
    private String nameFr;
    private String nameAr;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean active = true;

    public City() {
    }

    public City(String id, String nameFr, String nameAr, double latitude, double longitude) {
        this.id = id;
        this.nameFr = nameFr;
        this.nameAr = nameAr;
        this.latitude = BigDecimal.valueOf(latitude);
        this.longitude = BigDecimal.valueOf(longitude);
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

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return nameFr;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof City other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
