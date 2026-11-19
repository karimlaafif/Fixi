package ma.fixi.service.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import ma.fixi.dto.ArtisanSearchCriteria;
import ma.fixi.model.Appointment;
import ma.fixi.model.Artisan;
import ma.fixi.model.ArtisanStats;
import ma.fixi.model.Conversation;
import ma.fixi.model.Message;
import ma.fixi.model.ServiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcIteration2ServicesTest {

    private JdbcArtisanService artisanService;
    private JdbcRequestService requestService;
    private JdbcMessageService messageService;

    @BeforeEach
    void setUp() {
        artisanService = new JdbcArtisanService();
        requestService = new JdbcRequestService();
        messageService = new JdbcMessageService();
    }

    @Test
    void searchArtisans() {
        List<Artisan> list = artisanService.search(new ArtisanSearchCriteria("", null, null, 0, null, false, null));
        assertFalse(list.isEmpty());
    }

    @Test
    void findArtisanById() {
        Optional<Artisan> opt = artisanService.findById("u-art-ahmed");
        assertTrue(opt.isPresent());
        Artisan a = opt.get();
        assertEquals("Ahmed El Amine", a.getDisplayName());
        assertNotNull(a.getCity());
        assertFalse(a.getCoveredAreas().isEmpty());
    }

    @Test
    void getStatsAndAppointments() {
        ArtisanStats stats = artisanService.getStats("u-art-ahmed");
        assertNotNull(stats);
        assertTrue(stats.getCompletedInterventions() >= 26);

        List<Appointment> apps = artisanService.findUpcomingAppointments("u-art-ahmed", 5);
        assertNotNull(apps);
    }

    @Test
    void findRequestsByClient() {
        List<ServiceRequest> reqs = requestService.findByClient("u-client-sarah");
        assertFalse(reqs.isEmpty());
    }

    @Test
    void messageFlow() {
        Conversation conv = messageService.openWithArtisan("u-client-sarah", "u-art-ahmed");
        assertNotNull(conv);
        assertNotNull(conv.getId());

        Message sent = messageService.send(conv.getId(), "u-client-sarah", "Test message " + System.currentTimeMillis(), null);
        assertNotNull(sent.getId());

        List<Message> msgs = messageService.findMessages(conv.getId(), "u-art-ahmed");
        assertFalse(msgs.isEmpty());
    }
}
