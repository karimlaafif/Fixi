package ma.fixi.config;

import ma.fixi.service.Services;
import ma.fixi.service.jdbc.JdbcArtisanService;
import ma.fixi.service.jdbc.JdbcAuthService;
import ma.fixi.service.jdbc.JdbcCityService;
import ma.fixi.service.jdbc.JdbcMessageService;
import ma.fixi.service.jdbc.JdbcRequestService;
import ma.fixi.service.jdbc.JdbcTradeService;
import ma.fixi.service.mock.MockAuthService;

/**
 * Configuration des services de l'application Fixi.
 * Toutes les interfaces sont branchées sur les implémentations JDBC connectées à MySQL.
 */
public final class ServiceConfig {

    private ServiceConfig() {
    }

    public static void install() {
        Services.register(
                new JdbcAuthService(),
                new JdbcTradeService(),
                new JdbcCityService(),
                new JdbcArtisanService(),
                new JdbcRequestService(),
                new JdbcMessageService());
    }

    /**
     * Vrai si l'authentification est en mode démonstration fictif.
     */
    public static boolean isDemoMode() {
        return Services.auth() instanceof MockAuthService;
    }
}
