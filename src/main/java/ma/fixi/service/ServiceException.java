package ma.fixi.service;

/**
 * Erreur métier ou technique remontée par un service.
 * Le message est affiché tel quel à l'utilisateur : il doit être en français et compréhensible
 * (« Cet e-mail est déjà utilisé. »), jamais un message SQL brut.
 * Pour une erreur technique, passez l'exception d'origine en {@code cause} et un message générique.
 */
public class ServiceException extends RuntimeException {

    public ServiceException(String userMessage) {
        super(userMessage);
    }

    public ServiceException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
