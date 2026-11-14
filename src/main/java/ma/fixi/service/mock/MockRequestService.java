package ma.fixi.service.mock;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import ma.fixi.dto.NewRequestForm;
import ma.fixi.model.RequestStatus;
import ma.fixi.model.ServiceRequest;
import ma.fixi.model.User;
import ma.fixi.service.RequestService;
import ma.fixi.service.ServiceException;

/** Gestion fictive des demandes. Les règles de statut sont les mêmes que celles attendues côté base. */
public class MockRequestService implements RequestService {

    private final MockStore store;

    public MockRequestService(MockStore store) {
        this.store = store;
    }

    @Override
    public synchronized List<ServiceRequest> findByClient(String clientId) {
        return store.requests.stream()
                .filter(r -> r.getClientId().equals(clientId))
                .sorted(Comparator.comparing(ServiceRequest::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public synchronized ServiceRequest create(String clientId, NewRequestForm form) {
        User client = store.users.get(clientId);
        ServiceRequest r = new ServiceRequest();
        r.setId(MockStore.newId());
        r.setReference("D-" + store.nextReference++);
        r.setClientId(clientId);
        r.setClientName(client == null ? "" : client.getFullName());
        r.setTitle(form.title().trim());
        r.setTrade(form.trade());
        r.setDescription(form.description().trim());
        r.setCity(form.city());
        r.setAddressLine(form.addressLine());
        r.setBudget(form.budget());
        r.setPreferredDate(form.preferredDate());
        r.setUrgency(form.urgency());
        r.setStatus(RequestStatus.SENT);
        r.setCreatedAt(LocalDateTime.now());
        List<String> names = new ArrayList<>();
        for (File f : form.photos()) {
            names.add(f.getName());
        }
        r.setAttachmentNames(names);
        store.requests.add(r);
        return r;
    }

    @Override
    public synchronized ServiceRequest cancel(String requestId) {
        ServiceRequest r = find(requestId);
        if (!r.getStatus().canBeCancelledByClient()) {
            throw new ServiceException("Cette demande ne peut plus être annulée : les travaux ont commencé.");
        }
        r.setStatus(RequestStatus.CANCELLED);
        return r;
    }

    @Override
    public synchronized ServiceRequest changePreferredDate(String requestId, LocalDate newDate) {
        ServiceRequest r = find(requestId);
        if (!r.getStatus().canChangeDate()) {
            throw new ServiceException("La date ne peut plus être modifiée pour cette demande.");
        }
        if (newDate.isBefore(LocalDate.now())) {
            throw new ServiceException("Choisissez une date à partir d'aujourd'hui.");
        }
        r.setPreferredDate(newDate);
        return r;
    }

    @Override
    public synchronized ServiceRequest confirmCompletion(String requestId) {
        ServiceRequest r = find(requestId);
        if (!r.getStatus().canBeValidatedByClient()) {
            throw new ServiceException("L'artisan n'a pas encore déclaré les travaux terminés.");
        }
        r.setStatus(RequestStatus.VALIDATED);
        return r;
    }

    @Override
    public synchronized List<ServiceRequest> findIncomingForArtisan(String artisanId) {
        return List.copyOf(store.incoming.getOrDefault(artisanId, List.of()));
    }

    @Override
    public synchronized void decline(String artisanId, String requestId) {
        List<ServiceRequest> list = store.incoming.get(artisanId);
        if (list != null) {
            list.removeIf(r -> r.getId().equals(requestId));
        }
    }

    private ServiceRequest find(String requestId) {
        return store.requests.stream()
                .filter(r -> r.getId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Demande introuvable."));
    }
}
