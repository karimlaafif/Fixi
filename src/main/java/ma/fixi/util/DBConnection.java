package ma.fixi.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Ouvre les connexions MySQL.
 * Lit config.properties (à la racine du projet, non versionné) ;
 * à défaut, utilise config.example.properties.
 */
public final class DBConnection {

    private static final Properties CONFIG = load();

    private DBConnection() {
    }

    private static Properties load() {
        Properties p = new Properties();
        Path local = Path.of("config.properties");
        try {
            if (Files.exists(local)) {
                try (InputStream in = Files.newInputStream(local)) {
                    p.load(in);
                }
            } else {
                try (InputStream in = DBConnection.class.getResourceAsStream("/config.example.properties")) {
                    if (in != null) {
                        p.load(in);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire la configuration", e);
        }
        return p;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.user"),
                CONFIG.getProperty("db.password"));
    }
}
