package ma.fixi.dto;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.fixi.model.City;
import ma.fixi.model.Trade;
import ma.fixi.model.Urgency;

/**
 * Données saisies dans l'écran « Créer une demande de service ».
 *
 * @param budget          facultatif, en MAD
 * @param photos          fichiers choisis par l'utilisateur (PNG/JPG, 5 Mo max chacun)
 * @param targetArtisanId artisan choisi depuis son profil, ou {@code null} pour une demande ouverte
 */
public record NewRequestForm(
        String title,
        Trade trade,
        String description,
        City city,
        String addressLine,
        BigDecimal budget,
        LocalDate preferredDate,
        Urgency urgency,
        List<File> photos,
        String targetArtisanId) {
}
