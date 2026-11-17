package ma.fixi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import ma.fixi.model.Trade;
import ma.fixi.service.mock.MockStore;
import ma.fixi.service.mock.MockTradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comportement attendu de tout {@link TradeService}.
 *
 * <p>Équipe backend : pour tester {@code JdbcTradeService}, copiez cette classe et remplacez
 * {@link #createService()} (avec une base de test vide + le jeu d'essai {@code db/seed.sql}).</p>
 */
class TradeServiceContractTest {

    private TradeService service;

    protected TradeService createService() {
        return new MockTradeService(new MockStore());
    }

    @BeforeEach
    void setUp() {
        service = createService();
    }

    @Test
    void createGeneratesIdAndIsReadBack() {
        Trade created = service.create(new Trade(null, "Jardinage", "بستنة", true));
        assertNotNull(created.getId());
        assertTrue(service.findAll().stream().anyMatch(t -> t.getId().equals(created.getId())));
    }

    @Test
    void findAllIsSortedByFrenchName() {
        List<Trade> all = service.findAll();
        for (int i = 1; i < all.size(); i++) {
            assertTrue(all.get(i - 1).getNameFr().compareToIgnoreCase(all.get(i).getNameFr()) <= 0);
        }
    }

    @Test
    void updateChangesNameAndActiveFlag() {
        Trade created = service.create(new Trade(null, "Vitrerie", "زجاج", true));
        service.update(new Trade(created.getId(), "Vitrerie et miroiterie", "زجاج", false));
        Trade reloaded = service.findAll().stream().filter(t -> t.getId().equals(created.getId())).findFirst().orElseThrow();
        assertEquals("Vitrerie et miroiterie", reloaded.getNameFr());
        assertFalse(reloaded.isActive());
    }

    @Test
    void inactiveTradesAreHiddenFromClients() {
        Trade created = service.create(new Trade(null, "Ramonage", "تنظيف المداخن", false));
        assertTrue(service.findActive().stream().noneMatch(t -> t.getId().equals(created.getId())));
    }

    @Test
    void deleteRemovesUnusedTrade() {
        Trade created = service.create(new Trade(null, "Tapisserie", "تنجيد", true));
        service.delete(created.getId());
        assertTrue(service.findAll().stream().noneMatch(t -> t.getId().equals(created.getId())));
    }

    @Test
    void deleteRefusesTradeInUse() {
        Trade plumbing = service.findAll().stream().filter(t -> t.getNameFr().equals("Plomberie")).findFirst().orElseThrow();
        assertThrows(ServiceException.class, () -> service.delete(plumbing.getId()));
    }

    @Test
    void duplicateNameIsRefused() {
        assertThrows(ServiceException.class, () -> service.create(new Trade(null, "plomberie", "سباكة", true)));
    }
}
