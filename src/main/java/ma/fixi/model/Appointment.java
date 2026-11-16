package ma.fixi.model;

import java.time.LocalDateTime;

/** Rendez-vous d'intervention dans l'agenda de l'artisan. Correspond à la table {@code appointment}. */
public class Appointment {

    private String id;
    private String interventionId;
    private String title;
    private String clientName;
    private City city;
    private LocalDateTime startsAt;

    public Appointment() {
    }

    public Appointment(String id, String title, String clientName, City city, LocalDateTime startsAt) {
        this.id = id;
        this.title = title;
        this.clientName = clientName;
        this.city = city;
        this.startsAt = startsAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInterventionId() {
        return interventionId;
    }

    public void setInterventionId(String interventionId) {
        this.interventionId = interventionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }
}
