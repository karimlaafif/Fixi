package ma.fixi.service;

import java.time.LocalDate;
import java.util.List;
import ma.fixi.dto.NewRequestForm;
import ma.fixi.model.ServiceRequest;

/**
 * Demandes de service, côté client et côté artisan.
 * Tables : {@code service_request}, {@code request_recipient}, {@code request_attachment},
 * {@code status_event}.
 */
public interface RequestService {

    /** Demandes du client, de la plus récente à la plus ancienne. */
    List<ServiceRequest> findByClient(String clientId);

    /**
     * Crée et envoie la demande (statut {@code SENT}).
     * Si {@code form.targetArtisanId()} est renseigné, elle n'est envoyée qu'à cet artisan ;
     * sinon, aux artisans du métier et de la ville.
     *
     * @return la demande créée, avec son id et sa référence
     */
    ServiceRequest create(String clientId, NewRequestForm form);

    /**
     * Annule la demande.
     *
     * @throws ServiceException si le statut ne le permet plus ({@code RequestStatus.canBeCancelledByClient()})
     */
    ServiceRequest cancel(String requestId);

    /** Change la date souhaitée. */
    ServiceRequest changePreferredDate(String requestId, LocalDate newDate);

    /**
     * Le client confirme que les travaux sont terminés ({@code DONE_BY_ARTISAN → VALIDATED}).
     *
     * @throws ServiceException si l'artisan n'a pas encore déclaré les travaux terminés
     */
    ServiceRequest confirmCompletion(String requestId);

    /** Demandes reçues par l'artisan et auxquelles il n'a pas encore répondu. */
    List<ServiceRequest> findIncomingForArtisan(String artisanId);

    /** L'artisan décline une demande reçue ({@code request_recipient.response_status = DECLINED}). */
    void decline(String artisanId, String requestId);
}
