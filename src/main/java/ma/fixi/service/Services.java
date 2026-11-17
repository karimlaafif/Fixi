package ma.fixi.service;

import java.util.Objects;

/**
 * Point d'accès unique aux services pour les contrôleurs.
 *
 * <p>Les contrôleurs n'utilisent que les interfaces ({@code Services.trades().findAll()}),
 * jamais une implémentation précise. Le choix des implémentations (fictives ou base de données)
 * se fait à un seul endroit : {@link ma.fixi.config.ServiceConfig}.</p>
 */
public final class Services {

    private static AuthService auth;
    private static TradeService trades;
    private static CityService cities;
    private static ArtisanService artisans;
    private static RequestService requests;
    private static MessageService messages;

    private Services() {
    }

    public static void register(AuthService auth, TradeService trades, CityService cities,
                                ArtisanService artisans, RequestService requests, MessageService messages) {
        Services.auth = Objects.requireNonNull(auth);
        Services.trades = Objects.requireNonNull(trades);
        Services.cities = Objects.requireNonNull(cities);
        Services.artisans = Objects.requireNonNull(artisans);
        Services.requests = Objects.requireNonNull(requests);
        Services.messages = Objects.requireNonNull(messages);
    }

    public static AuthService auth() {
        return require(auth);
    }

    public static TradeService trades() {
        return require(trades);
    }

    public static CityService cities() {
        return require(cities);
    }

    public static ArtisanService artisans() {
        return require(artisans);
    }

    public static RequestService requests() {
        return require(requests);
    }

    public static MessageService messages() {
        return require(messages);
    }

    private static <T> T require(T service) {
        if (service == null) {
            throw new IllegalStateException("Services non initialisés : appelez ServiceConfig.install() au démarrage.");
        }
        return service;
    }
}
