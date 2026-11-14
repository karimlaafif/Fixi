package ma.fixi.service;

import java.util.List;
import ma.fixi.model.Trade;

/** CRUD des métiers. Table : {@code trade}. C'est la fonctionnalité CRUD demandée pour l'itération 1. */
public interface TradeService {

    /** Tous les métiers, actifs ou non, triés par nom français (écran d'administration). */
    List<Trade> findAll();

    /** Métiers actifs seulement, triés par nom français (listes déroulantes des clients). */
    List<Trade> findActive();

    /** Insère le métier. Génère l'id (UUID) s'il est vide et renvoie le métier enregistré. */
    Trade create(Trade trade);

    /** Met à jour le métier dont l'id est {@code trade.getId()}. */
    Trade update(Trade trade);

    /**
     * Supprime le métier.
     *
     * @throws ServiceException si le métier est encore utilisé par des artisans ou des demandes
     */
    void delete(String tradeId);
}
