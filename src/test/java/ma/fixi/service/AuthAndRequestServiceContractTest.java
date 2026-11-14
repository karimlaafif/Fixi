package ma.fixi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import ma.fixi.dto.NewRequestForm;
import ma.fixi.dto.RegistrationForm;
import ma.fixi.model.ClientType;
import ma.fixi.model.RequestStatus;
import ma.fixi.model.Role;
import ma.fixi.model.ServiceRequest;
import ma.fixi.model.Urgency;
import ma.fixi.model.User;
import ma.fixi.service.mock.MockAuthService;
import ma.fixi.service.mock.MockCityService;
import ma.fixi.service.mock.MockRequestService;
import ma.fixi.service.mock.MockStore;
import ma.fixi.service.mock.MockTradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Comportement attendu de {@link AuthService} et {@link RequestService} (mêmes règles côté base). */
class AuthAndRequestServiceContractTest {

    private AuthService auth;
    private RequestService requests;
    private TradeService trades;
    private CityService cities;

    @BeforeEach
    void setUp() {
        MockStore store = new MockStore();
        auth = new MockAuthService(store);
        requests = new MockRequestService(store);
        trades = new MockTradeService(store);
        cities = new MockCityService(store);
    }

    @Test
    void loginWithDemoAccount() {
        User u = auth.login("client@fixi.ma", MockStore.DEMO_PASSWORD);
        assertEquals(Role.CLIENT, u.getPrimaryRole());
    }

    @Test
    void loginWithWrongPasswordFailsWithReadableMessage() {
        ServiceException e = assertThrows(ServiceException.class, () -> auth.login("client@fixi.ma", "mauvais"));
        assertEquals("E-mail ou mot de passe incorrect.", e.getMessage());
    }

    @Test
    void registerThenLogin() {
        auth.register(new RegistrationForm("Nadia Test", "nadia@test.ma", null, "motdepasse1", Role.CLIENT,
                ClientType.SYNDIC));
        User u = auth.login("nadia@test.ma", "motdepasse1");
        assertEquals(ClientType.SYNDIC, u.getClientType());
    }

    @Test
    void registerRefusesDuplicateEmail() {
        assertThrows(ServiceException.class, () -> auth.register(new RegistrationForm("X", "client@fixi.ma", null,
                "motdepasse1", Role.CLIENT, ClientType.PARTICULIER)));
    }

    @Test
    void createdRequestIsSentAndListedFirst() {
        NewRequestForm form = new NewRequestForm("Prise qui ne marche plus", trades.findActive().get(0),
                "La prise du salon ne fonctionne plus.", cities.findActive().get(0), null, null,
                LocalDate.now().plusDays(2), Urgency.NORMAL, List.of(), null);
        ServiceRequest created = requests.create(MockStore.CLIENT_ID, form);
        assertNotNull(created.getReference());
        assertEquals(RequestStatus.SENT, created.getStatus());
        assertEquals(created.getId(), requests.findByClient(MockStore.CLIENT_ID).get(0).getId());
    }

    @Test
    void cancelIsRefusedOnceWorkHasStarted() {
        ServiceRequest inProgress = byStatus(RequestStatus.IN_PROGRESS);
        assertThrows(ServiceException.class, () -> requests.cancel(inProgress.getId()));
    }

    @Test
    void confirmCompletionOnlyAfterArtisanDeclaredDone() {
        ServiceRequest toValidate = byStatus(RequestStatus.DONE_BY_ARTISAN);
        assertEquals(RequestStatus.VALIDATED, requests.confirmCompletion(toValidate.getId()).getStatus());
        ServiceRequest scheduled = byStatus(RequestStatus.SCHEDULED);
        assertThrows(ServiceException.class, () -> requests.confirmCompletion(scheduled.getId()));
    }

    @Test
    void declinedRequestDisappearsForArtisan() {
        List<ServiceRequest> before = requests.findIncomingForArtisan(MockStore.ARTISAN_ID);
        requests.decline(MockStore.ARTISAN_ID, before.get(0).getId());
        assertEquals(before.size() - 1, requests.findIncomingForArtisan(MockStore.ARTISAN_ID).size());
        assertTrue(requests.findIncomingForArtisan(MockStore.ARTISAN_ID).stream()
                .noneMatch(r -> r.getId().equals(before.get(0).getId())));
    }

    private ServiceRequest byStatus(RequestStatus status) {
        return requests.findByClient(MockStore.CLIENT_ID).stream()
                .filter(r -> r.getStatus() == status).findFirst().orElseThrow();
    }
}
