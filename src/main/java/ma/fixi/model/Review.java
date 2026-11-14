package ma.fixi.model;

import java.time.LocalDate;

/** Avis publié après une intervention clôturée. Correspond à la future table {@code review}. */
public class Review {

    private String authorName;
    private int rating;
    private String comment;
    private LocalDate publishedOn;

    public Review() {
    }

    public Review(String authorName, int rating, String comment, LocalDate publishedOn) {
        this.authorName = authorName;
        this.rating = rating;
        this.comment = comment;
        this.publishedOn = publishedOn;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDate getPublishedOn() {
        return publishedOn;
    }

    public void setPublishedOn(LocalDate publishedOn) {
        this.publishedOn = publishedOn;
    }
}
