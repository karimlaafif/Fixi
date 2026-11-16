package ma.fixi.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * Exécute un appel de service hors du fil JavaFX, puis revient sur le fil JavaFX pour mettre à jour l'écran.
 *
 * <p>Pourquoi : une requête SQL qui prend 1 seconde ne doit pas figer la fenêtre.
 * Les services fictifs répondent instantanément, mais les écrans sont déjà prêts pour une vraie base.</p>
 *
 * <pre>
 * Async.run(() -> Services.trades().findAll(),
 *           trades -> table.getItems().setAll(trades),
 *           error  -> Toast.error(root, error.getMessage()));
 * </pre>
 */
public final class Async {

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "fixi-service");
        t.setDaemon(true);
        return t;
    });

    private Async() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        POOL.submit(() -> {
            try {
                T result = work.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Throwable e) {
                if (!(e instanceof ma.fixi.service.ServiceException)) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> onError.accept(e));
            }
        });
    }

    /** Variante pour un appel sans résultat (suppression, refus…). */
    public static void run(ThrowingRunnable work, Runnable onSuccess, Consumer<Throwable> onError) {
        run(() -> {
            work.run();
            return null;
        }, ignored -> onSuccess.run(), onError);
    }

    /** Message lisible pour l'utilisateur, quelle que soit l'erreur. */
    public static String userMessage(Throwable e) {
        if (e instanceof ma.fixi.service.ServiceException && e.getMessage() != null) {
            return e.getMessage();
        }
        return "Une erreur inattendue s'est produite. Réessayez dans un instant.";
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
