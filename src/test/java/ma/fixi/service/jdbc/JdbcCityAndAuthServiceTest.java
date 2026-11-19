package ma.fixi.service.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import ma.fixi.dto.RegistrationForm;
import ma.fixi.model.City;
import ma.fixi.model.ClientType;
import ma.fixi.model.Role;
import ma.fixi.model.User;
import ma.fixi.service.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcCityAndAuthServiceTest {

    private JdbcCityService cityService;
    private JdbcAuthService authService;

    @BeforeEach
    void setUp() {
        cityService = new JdbcCityService();
        authService = new JdbcAuthService();
    }

    @Test
    void findActiveCities() {
        List<City> cities = cityService.findActive();
        assertFalse(cities.isEmpty(), "La liste des villes actives ne doit pas être vide.");
        for (City c : cities) {
            assertTrue(c.isActive());
            assertNotNull(c.getNameFr());
            assertNotNull(c.getLatitude());
            assertNotNull(c.getLongitude());
        }
    }

    @Test
    void loginWithDemoClientAccount() {
        User u = authService.login("client@fixi.ma", "fixi1234");
        assertNotNull(u);
        assertEquals("client@fixi.ma", u.getEmail());
        assertTrue(u.hasRole(Role.CLIENT));
    }

    @Test
    void loginWithWrongPasswordFails() {
        ServiceException e = assertThrows(ServiceException.class, () -> authService.login("client@fixi.ma", "mauvais_pass"));
        assertEquals("E-mail ou mot de passe incorrect.", e.getMessage());
    }

    @Test
    void registerAndLoginNewUser() {
        long ts = System.currentTimeMillis();
        String testEmail = "testuser" + ts + "@test.ma";
        String testPhone = "+2126" + String.format("%08d", ts % 100000000L);
        RegistrationForm form = new RegistrationForm("User Test", testEmail, testPhone, "monPass123", Role.CLIENT, ClientType.PARTICULIER);

        User registered = authService.register(form);
        assertNotNull(registered.getId());
        assertEquals(testEmail, registered.getEmail());

        User loggedIn = authService.login(testEmail, "monPass123");
        assertEquals(registered.getId(), loggedIn.getId());
        assertEquals("User Test", loggedIn.getFullName());
    }
}
