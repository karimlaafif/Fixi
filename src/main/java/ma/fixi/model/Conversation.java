package ma.fixi.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Conversation privée entre un client et un artisan autour d'une demande.
 * Correspond à la table {@code conversation}. Les champs « other… » décrivent
 * l'interlocuteur vu par l'utilisateur connecté.
 */
public class Conversation {

    private String id;
    private String requestId;
    private String otherPartyId;
    private String otherPartyName;
    /** Sous-titre de l'interlocuteur : métier de l'artisan, ou « Client ». */
    private String otherPartySubtitle;
    /** Identifiant de l'artisan, pour ouvrir son profil ; {@code null} si l'interlocuteur est un client. */
    private String artisanId;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private int unreadCount;

    public Conversation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getOtherPartyId() {
        return otherPartyId;
    }

    public void setOtherPartyId(String otherPartyId) {
        this.otherPartyId = otherPartyId;
    }

    public String getOtherPartyName() {
        return otherPartyName;
    }

    public void setOtherPartyName(String otherPartyName) {
        this.otherPartyName = otherPartyName;
    }

    public String getOtherPartySubtitle() {
        return otherPartySubtitle;
    }

    public void setOtherPartySubtitle(String otherPartySubtitle) {
        this.otherPartySubtitle = otherPartySubtitle;
    }

    public String getArtisanId() {
        return artisanId;
    }

    public void setArtisanId(String artisanId) {
        this.artisanId = artisanId;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Conversation other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
