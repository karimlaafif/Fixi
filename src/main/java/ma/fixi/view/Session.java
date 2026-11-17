package ma.fixi.view;

import ma.fixi.model.User;

/** Utilisateur connecté pour la durée de l'exécution. */
public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static User user() {
        if (currentUser == null) {
            throw new IllegalStateException("Aucun utilisateur connecté.");
        }
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void start(User user) {
        currentUser = user;
    }

    public static void end() {
        currentUser = null;
    }
}
