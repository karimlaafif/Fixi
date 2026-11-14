package ma.fixi.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Profil d'un artisan tel qu'affiché dans l'interface.
 * Regroupe des données venant de {@code artisan_profile}, {@code artisan_trade},
 * {@code artisan_coverage} et de la moyenne des avis.
 */
public class Artisan {

    /** Identifiant du compte ({@code artisan_profile.user_id}). */
    private String id;
    private String displayName;
    private Trade trade;
    private City city;
    private String description;
    private boolean verified;
    private double averageRating;
    private int reviewCount;
    private int yearsOfExperience;
    /** Tarif indicatif minimum en MAD ({@code artisan_trade.indicative_rate_min}). */
    private BigDecimal priceFrom;
    private Availability availability = Availability.LATER;
    private List<String> coveredAreas = new ArrayList<>();
    private List<Review> reviews = new ArrayList<>();

    public Artisan() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public BigDecimal getPriceFrom() {
        return priceFrom;
    }

    public void setPriceFrom(BigDecimal priceFrom) {
        this.priceFrom = priceFrom;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }

    public List<String> getCoveredAreas() {
        return coveredAreas;
    }

    public void setCoveredAreas(List<String> coveredAreas) {
        this.coveredAreas = new ArrayList<>(coveredAreas);
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = new ArrayList<>(reviews);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Artisan other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
