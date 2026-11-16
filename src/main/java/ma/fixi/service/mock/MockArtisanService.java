package ma.fixi.service.mock;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import ma.fixi.dto.ArtisanSearchCriteria;
import ma.fixi.model.Appointment;
import ma.fixi.model.Artisan;
import ma.fixi.model.ArtisanStats;
import ma.fixi.model.RequestStatus;
import ma.fixi.service.ArtisanService;

/** Recherche fictive des artisans (filtrage en mémoire). */
public class MockArtisanService implements ArtisanService {

    private final MockStore store;

    public MockArtisanService(MockStore store) {
        this.store = store;
    }

    @Override
    public synchronized List<Artisan> search(ArtisanSearchCriteria c) {
        Stream<Artisan> s = store.artisans.stream();
        if (c.text() != null && !c.text().isBlank()) {
            String q = normalize(c.text());
            s = s.filter(a -> normalize(a.getDisplayName()).contains(q) || normalize(a.getTrade().getNameFr()).contains(q));
        }
        if (c.trade() != null) {
            s = s.filter(a -> a.getTrade().equals(c.trade()));
        }
        if (c.city() != null) {
            s = s.filter(a -> a.getCity().equals(c.city()) || a.getCoveredAreas().contains(c.city().getNameFr()));
        }
        if (c.minRating() > 0) {
            s = s.filter(a -> a.getAverageRating() >= c.minRating());
        }
        if (c.availableBy() != null) {
            s = s.filter(a -> a.getAvailability().isAtLeast(c.availableBy()));
        }
        if (c.verifiedOnly()) {
            s = s.filter(Artisan::isVerified);
        }
        Comparator<Artisan> order = switch (c.sort() == null ? ArtisanSearchCriteria.Sort.RATING : c.sort()) {
            case RATING -> Comparator.comparingDouble(Artisan::getAverageRating).reversed();
            case PRICE -> Comparator.comparing(Artisan::getPriceFrom);
            case EXPERIENCE -> Comparator.comparingInt(Artisan::getYearsOfExperience).reversed();
        };
        return s.sorted(order).toList();
    }

    @Override
    public synchronized Optional<Artisan> findById(String artisanId) {
        return Optional.ofNullable(store.artisan(artisanId));
    }

    @Override
    public synchronized List<Artisan> findTopRated(int limit) {
        return store.artisans.stream()
                .filter(Artisan::isVerified)
                .sorted(Comparator.comparingDouble(Artisan::getAverageRating).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized ArtisanStats getStats(String artisanId) {
        Artisan a = store.artisan(artisanId);
        int incoming = store.incoming.getOrDefault(artisanId, List.of()).size();
        long planned = store.appointments.stream().filter(ap -> ap.getStartsAt().isAfter(LocalDateTime.now())).count();
        long done = store.requests.stream()
                .filter(r -> r.getAssignedArtisan() != null && r.getAssignedArtisan().getId().equals(artisanId))
                .filter(r -> r.getStatus() == RequestStatus.VALIDATED || r.getStatus() == RequestStatus.CLOSED)
                .count();
        return new ArtisanStats(incoming, (int) planned, 26 + (int) done,
                a == null ? 0 : a.getAverageRating(), a == null ? 0 : a.getReviewCount());
    }

    @Override
    public synchronized List<Appointment> findUpcomingAppointments(String artisanId, int limit) {
        return store.appointments.stream()
                .filter(ap -> ap.getStartsAt().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Appointment::getStartsAt))
                .limit(limit)
                .toList();
    }

    /** Minuscules sans accents, pour que « electricite » trouve « Électricité ». */
    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
