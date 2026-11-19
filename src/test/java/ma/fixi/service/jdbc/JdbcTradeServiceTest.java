package ma.fixi.service.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.Collator;
import java.util.List;
import java.util.Locale;
import ma.fixi.model.Trade;
import ma.fixi.service.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcTradeServiceTest {

    private JdbcTradeService service;

    @BeforeEach
    void setUp() {
        service = new JdbcTradeService();
    }

    @Test
    void findAllIsSortedByFrenchName() {
        List<Trade> all = service.findAll();
        assertFalse(all.isEmpty(), "La table trade ne doit pas être vide.");
        Collator collator = Collator.getInstance(Locale.FRANCE);
        collator.setStrength(Collator.PRIMARY);
        for (int i = 1; i < all.size(); i++) {
            assertTrue(collator.compare(all.get(i - 1).getNameFr(), all.get(i).getNameFr()) <= 0,
                    "Ordre incorrect : " + all.get(i - 1).getNameFr() + " devrait précéder " + all.get(i).getNameFr());
        }
    }

    @Test
    void findActiveReturnsOnlyActive() {
        List<Trade> active = service.findActive();
        assertFalse(active.isEmpty());
        for (Trade t : active) {
            assertTrue(t.isActive());
        }
    }

    @Test
    void createUpdateAndDelete() {
        String testName = "Test-Metier-" + System.currentTimeMillis();
        Trade created = service.create(new Trade(null, testName, "تجربة", true));
        assertNotNull(created.getId());
        assertEquals(testName, created.getNameFr());

        // Update
        Trade updated = service.update(new Trade(created.getId(), testName + "-Modif", "تجربة 2", false));
        assertEquals(testName + "-Modif", updated.getNameFr());
        assertFalse(updated.isActive());

        // Delete
        service.delete(created.getId());
        assertTrue(service.findAll().stream().noneMatch(t -> t.getId().equals(created.getId())));
    }

    @Test
    void duplicateNameIsRefused() {
        Trade first = service.findAll().get(0);
        assertThrows(ServiceException.class, () -> service.create(new Trade(null, first.getNameFr(), "تجربة", true)));
    }

    @Test
    void deleteRefusesTradeInUse() {
        Trade plumbing = service.findAll().stream()
                .filter(t -> t.getNameFr().contains("Plomb"))
                .findFirst()
                .orElse(null);
        if (plumbing != null) {
            assertThrows(ServiceException.class, () -> service.delete(plumbing.getId()));
        }
    }
}
