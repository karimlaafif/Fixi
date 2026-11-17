package ma.fixi.service.mock;

import java.util.Comparator;
import java.util.List;
import ma.fixi.model.Trade;
import ma.fixi.service.ServiceException;
import ma.fixi.service.TradeService;

/** CRUD fictif des métiers. */
public class MockTradeService implements TradeService {

    private static final Comparator<Trade> BY_NAME = Comparator.comparing(Trade::getNameFr, String.CASE_INSENSITIVE_ORDER);

    private final MockStore store;

    public MockTradeService(MockStore store) {
        this.store = store;
    }

    @Override
    public synchronized List<Trade> findAll() {
        return store.trades.stream().map(MockTradeService::copy).sorted(BY_NAME).toList();
    }

    @Override
    public synchronized List<Trade> findActive() {
        return store.trades.stream().filter(Trade::isActive).map(MockTradeService::copy).sorted(BY_NAME).toList();
    }

    @Override
    public synchronized Trade create(Trade trade) {
        ensureUniqueName(trade);
        Trade saved = copy(trade);
        if (saved.getId() == null || saved.getId().isBlank()) {
            saved.setId(MockStore.newId());
        }
        store.trades.add(saved);
        return copy(saved);
    }

    @Override
    public synchronized Trade update(Trade trade) {
        ensureUniqueName(trade);
        for (int i = 0; i < store.trades.size(); i++) {
            if (store.trades.get(i).getId().equals(trade.getId())) {
                store.trades.set(i, copy(trade));
                return copy(trade);
            }
        }
        throw new ServiceException("Ce métier n'existe plus. Actualisez la liste.");
    }

    @Override
    public synchronized void delete(String tradeId) {
        boolean used = store.artisans.stream().anyMatch(a -> a.getTrade().getId().equals(tradeId))
                || store.requests.stream().anyMatch(r -> r.getTrade().getId().equals(tradeId));
        if (used) {
            throw new ServiceException("Ce métier est utilisé par des artisans ou des demandes. Désactivez-le plutôt.");
        }
        store.trades.removeIf(t -> t.getId().equals(tradeId));
    }

    private void ensureUniqueName(Trade trade) {
        boolean duplicate = store.trades.stream()
                .anyMatch(t -> !t.getId().equals(trade.getId()) && t.getNameFr().equalsIgnoreCase(trade.getNameFr().trim()));
        if (duplicate) {
            throw new ServiceException("Un métier porte déjà ce nom.");
        }
    }

    /** Copie défensive : l'écran ne modifie jamais directement les données stockées. */
    private static Trade copy(Trade t) {
        return new Trade(t.getId(), t.getNameFr().trim(), t.getNameAr().trim(), t.isActive());
    }
}
