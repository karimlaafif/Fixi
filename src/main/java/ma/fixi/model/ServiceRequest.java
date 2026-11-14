package ma.fixi.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Demande de service publiée par un client. Correspond à la table {@code service_request}
 * (plus le devis accepté et l'artisan assigné, pour l'affichage).
 */
public class ServiceRequest {

    private String id;
    /** Référence lisible affichée à l'utilisateur, par exemple « D-4587 ». */
    private String reference;
    private String clientId;
    private String clientName;
    private String title;
    private String description;
    private Trade trade;
    private City city;
    private String addressLine;
    private Urgency urgency = Urgency.NORMAL;
    private BigDecimal budget;
    private LocalDate preferredDate;
    private LocalDateTime createdAt;
    private RequestStatus status = RequestStatus.SENT;
    /** Artisan dont le devis a été accepté ; {@code null} tant qu'aucun devis n'est accepté. */
    private Artisan assignedArtisan;
    /** Montant total TTC du devis accepté ; {@code null} si aucun. */
    private BigDecimal acceptedQuoteTotal;
    private int quoteCount;
    private List<String> attachmentNames = new ArrayList<>();

    public ServiceRequest() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(LocalDate preferredDate) {
        this.preferredDate = preferredDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Artisan getAssignedArtisan() {
        return assignedArtisan;
    }

    public void setAssignedArtisan(Artisan assignedArtisan) {
        this.assignedArtisan = assignedArtisan;
    }

    public BigDecimal getAcceptedQuoteTotal() {
        return acceptedQuoteTotal;
    }

    public void setAcceptedQuoteTotal(BigDecimal acceptedQuoteTotal) {
        this.acceptedQuoteTotal = acceptedQuoteTotal;
    }

    public int getQuoteCount() {
        return quoteCount;
    }

    public void setQuoteCount(int quoteCount) {
        this.quoteCount = quoteCount;
    }

    public List<String> getAttachmentNames() {
        return attachmentNames;
    }

    public void setAttachmentNames(List<String> attachmentNames) {
        this.attachmentNames = new ArrayList<>(attachmentNames);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ServiceRequest other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
