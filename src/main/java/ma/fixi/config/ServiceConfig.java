package ma.fixi.config;

import ma.fixi.service.Services;
import ma.fixi.service.jdbc.JdbcAuthService;
import ma.fixi.service.jdbc.JdbcCityService;
import ma.fixi.service.jdbc.JdbcTradeService;
import ma.fixi.service.mock.MockArtisanService;
import ma.fixi.service.mock.MockAuthService;
import ma.fixi.service.mock.MockCityService;
import ma.fixi.service.mock.MockMessageService;
import ma.fixi.service.mock.MockRequestService;
import ma.fixi.service.mock.MockStore;
import ma.fixi.service.mock.MockTradeService;

/**
 * LE seul fichier à modifier pour brancher la base de données.
 */
public final class ServiceConfig {

    private ServiceConfig() {
    }

    public static void install() {
        MockStore store = new MockStore();

        Services.register(
                new JdbcAuthService(),
                new JdbcTradeService(),
                new JdbcCityService(),
                new MockArtisanService(store),
                new MockRequestService(store),
                new MockMessageService(store));
    }

    /**
     * Vrai tant que l'authentification est fictive : l'écran de connexion affiche alors
     * les comptes de démonstration. Passe à faux automatiquement dès que {@code MockAuthService} est remplacé.
     */
    public static boolean isDemoMode() {
        return Services.auth() instanceof MockAuthService;
    }
}
