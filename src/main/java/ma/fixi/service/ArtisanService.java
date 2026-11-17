package ma.fixi.service;

import java.util.List;
import java.util.Optional;
import ma.fixi.dto.ArtisanSearchCriteria;
import ma.fixi.model.Artisan;
import ma.fixi.model.ArtisanStats;
import ma.fixi.model.Appointment;

/**
 * Recherche et consultation des artisans, et données de l'espace artisan.
 * Tables : {@code artisan_profile}, {@code artisan_trade}, {@code artisan_coverage}, {@code review},
 * {@code appointment}.
 */
public interface ArtisanService {

    /** Artisans correspondant aux filtres, triés selon {@code criteria.sort()}. */
    List<Artisan> search(ArtisanSearchCriteria criteria);

    /** Profil complet (avec zones couvertes et avis), ou vide s'il n'existe pas. */
    Optional<Artisan> findById(String artisanId);

    /** Les artisans les mieux notés, pour l'accueil client. */
    List<Artisan> findTopRated(int limit);

    /** Chiffres du tableau de bord de l'artisan. */
    ArtisanStats getStats(String artisanId);

    /** Prochains rendez-vous de l'artisan, du plus proche au plus lointain. */
    List<Appointment> findUpcomingAppointments(String artisanId, int limit);
}
