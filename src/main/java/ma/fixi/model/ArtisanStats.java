package ma.fixi.model;

/** Chiffres du tableau de bord artisan. Calculés à partir des demandes et interventions. */
public class ArtisanStats {

    private int newRequests;
    private int plannedInterventions;
    private int completedInterventions;
    private double averageRating;
    private int reviewCount;

    public ArtisanStats() {
    }

    public ArtisanStats(int newRequests, int plannedInterventions, int completedInterventions,
                        double averageRating, int reviewCount) {
        this.newRequests = newRequests;
        this.plannedInterventions = plannedInterventions;
        this.completedInterventions = completedInterventions;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public int getNewRequests() {
        return newRequests;
    }

    public void setNewRequests(int newRequests) {
        this.newRequests = newRequests;
    }

    public int getPlannedInterventions() {
        return plannedInterventions;
    }

    public void setPlannedInterventions(int plannedInterventions) {
        this.plannedInterventions = plannedInterventions;
    }

    public int getCompletedInterventions() {
        return completedInterventions;
    }

    public void setCompletedInterventions(int completedInterventions) {
        this.completedInterventions = completedInterventions;
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
}
