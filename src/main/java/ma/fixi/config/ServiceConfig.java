package ma.fixi.config;

import ma.fixi.service.Services;
import ma.fixi.service.mock.MockArtisanService;
import ma.fixi.service.mock.MockAuthService;
import ma.fixi.service.mock.MockCityService;
import ma.fixi.service.mock.MockMessageService;
import ma.fixi.service.mock.MockRequestService;
import ma.fixi.service.mock.MockStore;
import ma.fixi.service.mock.MockTradeService;

/**
 * LE seul fichier à modifier pour brancher la base de données.
 *
 * <p>Aujourd'hui, toutes les implémentations sont fictives (données en mémoire, perdues à la fermeture).
 * Pour brancher MySQL, remplacez une ligne à la fois, par exemple :</p>
 *
 * <pre>
 *   new MockTradeService(store)   →   new JdbcTradeService()
 * </pre>
 *
 * <p>Les écrans ne changent pas : ils ne connaissent que les interfaces de {@code ma.fixi.service}.</p>
 */
public final class ServiceConfig {

    private ServiceConfig() {
    }

    public static void install() {
        MockStore store = new MockStore();

        Services.register(
                new MockAuthService(store),      // TODO backend : JdbcAuthService
                new MockTradeService(store),     // TODO backend : JdbcTradeService  (itération 1)
                new MockCityService(store),      // TODO backend : JdbcCityService   (itération 1)
                new MockArtisanService(store),   // TODO backend : JdbcArtisanService
                new MockRequestService(store),   // TODO backend : JdbcRequestService
                new MockMessageService(store));  // TODO backend : JdbcMessageService
    }

    /**
     * Vrai tant que l'authentification est fictive : l'écran de connexion affiche alors
     * les comptes de démonstration. Passe à faux automatiquement dès que {@code MockAuthService} est remplacé.
     */
    public static boolean isDemoMode() {
        return Services.auth() instanceof MockAuthService;
    }
}
