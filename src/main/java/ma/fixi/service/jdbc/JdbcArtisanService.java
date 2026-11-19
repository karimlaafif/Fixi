package ma.fixi.service.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import ma.fixi.dto.ArtisanSearchCriteria;
import ma.fixi.model.Appointment;
import ma.fixi.model.Artisan;
import ma.fixi.model.ArtisanStats;
import ma.fixi.model.Availability;
import ma.fixi.model.City;
import ma.fixi.model.Review;
import ma.fixi.model.Trade;
import ma.fixi.service.ArtisanService;
import ma.fixi.service.ServiceException;
import ma.fixi.util.DBConnection;

/** Recherche et gestion des profils artisans via MySQL. */
public class JdbcArtisanService implements ArtisanService {

    @Override
    public List<Artisan> search(ArtisanSearchCriteria c) {
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT ap.user_id, ap.display_name, ap.description, ap.years_of_experience, " +
                "ap.verification_status, ap.average_rating, ap.review_count, ap.next_available_on, " +
                "c.id AS city_id, c.name_fr AS city_name_fr, c.name_ar AS city_name_ar, c.latitude, c.longitude, c.is_active AS city_active, " +
                "t.id AS trade_id, t.name_fr AS trade_name_fr, t.name_ar AS trade_name_ar, t.is_active AS trade_active, " +
                "at.indicative_rate_min " +
                "FROM artisan_profile ap " +
                "JOIN city c ON ap.city_id = c.id " +
                "LEFT JOIN artisan_trade at ON ap.user_id = at.artisan_id " +
                "LEFT JOIN trade t ON at.trade_id = t.id " +
                "LEFT JOIN artisan_coverage ac ON ap.user_id = ac.artisan_id " +
                "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (c.text() != null && !c.text().isBlank()) {
            sql.append("AND (LOWER(ap.display_name) LIKE ? OR LOWER(t.name_fr) LIKE ?) ");
            String pattern = "%" + c.text().trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (c.trade() != null && c.trade().getId() != null) {
            sql.append("AND t.id = ? ");
            params.add(c.trade().getId());
        }

        if (c.city() != null && c.city().getId() != null) {
            sql.append("AND (c.id = ? OR ac.city_id = ?) ");
            params.add(c.city().getId());
            params.add(c.city().getId());
        }

        if (c.minRating() > 0) {
            sql.append("AND ap.average_rating >= ? ");
            params.add(c.minRating());
        }

        if (c.verifiedOnly()) {
            sql.append("AND ap.verification_status = 'VERIFIED' ");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            List<Artisan> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Artisan a = mapArtisanBasic(rs);
                    list.add(a);
                }
            }

            // Filtrage disponibilité en mémoire si demandé
            if (c.availableBy() != null) {
                list = list.stream()
                        .filter(a -> a.getAvailability().isAtLeast(c.availableBy()))
                        .toList();
            }

            // Tri
            Comparator<Artisan> order = switch (c.sort() == null ? ArtisanSearchCriteria.Sort.RATING : c.sort()) {
                case RATING -> Comparator.comparingDouble(Artisan::getAverageRating).reversed();
                case PRICE -> Comparator.comparing(a -> a.getPriceFrom() == null ? BigDecimal.valueOf(999999) : a.getPriceFrom());
                case EXPERIENCE -> Comparator.comparingInt(Artisan::getYearsOfExperience).reversed();
            };

            return list.stream().sorted(order).toList();
        } catch (SQLException e) {
            throw new ServiceException("Impossible d'effectuer la recherche d'artisans.", e);
        }
    }

    @Override
    public Optional<Artisan> findById(String artisanId) {
        String sql = "SELECT ap.user_id, ap.display_name, ap.description, ap.years_of_experience, " +
                "ap.verification_status, ap.average_rating, ap.review_count, ap.next_available_on, " +
                "c.id AS city_id, c.name_fr AS city_name_fr, c.name_ar AS city_name_ar, c.latitude, c.longitude, c.is_active AS city_active, " +
                "t.id AS trade_id, t.name_fr AS trade_name_fr, t.name_ar AS trade_name_ar, t.is_active AS trade_active, " +
                "at.indicative_rate_min " +
                "FROM artisan_profile ap " +
                "JOIN city c ON ap.city_id = c.id " +
                "LEFT JOIN artisan_trade at ON ap.user_id = at.artisan_id " +
                "LEFT JOIN trade t ON at.trade_id = t.id " +
                "WHERE ap.user_id = ? ";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, artisanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Artisan artisan = mapArtisanBasic(rs);
                artisan.setCoveredAreas(loadCoveredAreas(conn, artisanId));
                artisan.setReviews(loadReviews(conn, artisanId));
                return Optional.of(artisan);
            }
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger le profil de l'artisan.", e);
        }
    }

    @Override
    public List<Artisan> findTopRated(int limit) {
        String sql = "SELECT ap.user_id, ap.display_name, ap.description, ap.years_of_experience, " +
                "ap.verification_status, ap.average_rating, ap.review_count, ap.next_available_on, " +
                "c.id AS city_id, c.name_fr AS city_name_fr, c.name_ar AS city_name_ar, c.latitude, c.longitude, c.is_active AS city_active, " +
                "t.id AS trade_id, t.name_fr AS trade_name_fr, t.name_ar AS trade_name_ar, t.is_active AS trade_active, " +
                "at.indicative_rate_min " +
                "FROM artisan_profile ap " +
                "JOIN city c ON ap.city_id = c.id " +
                "LEFT JOIN artisan_trade at ON ap.user_id = at.artisan_id " +
                "LEFT JOIN trade t ON at.trade_id = t.id " +
                "WHERE ap.verification_status = 'VERIFIED' " +
                "ORDER BY ap.average_rating DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            List<Artisan> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapArtisanBasic(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger les artisans les mieux notés.", e);
        }
    }

    @Override
    public ArtisanStats getStats(String artisanId) {
        int incoming = 0;
        int planned = 0;
        int done = 0;
        double avgRating = 0.0;
        int reviewCount = 0;

        try (Connection conn = DBConnection.getConnection()) {
            // Profil pour note et nombre d'avis
            String sqlProfile = "SELECT average_rating, review_count FROM artisan_profile WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlProfile)) {
                ps.setString(1, artisanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        avgRating = rs.getDouble("average_rating");
                        reviewCount = rs.getInt("review_count");
                    }
                }
            }

            // Demandes en attente
            String sqlIncoming = "SELECT COUNT(*) FROM request_recipient WHERE artisan_id = ? AND response_status = 'PENDING'";
            try (PreparedStatement ps = conn.prepareStatement(sqlIncoming)) {
                ps.setString(1, artisanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) incoming = rs.getInt(1);
                }
            }

            // Rendez-vous futurs
            String sqlPlanned = "SELECT COUNT(*) FROM appointment WHERE artisan_id = ? AND starts_at > NOW()";
            try (PreparedStatement ps = conn.prepareStatement(sqlPlanned)) {
                ps.setString(1, artisanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) planned = rs.getInt(1);
                }
            }

            // Interventions terminées
            String sqlDone = "SELECT COUNT(*) FROM service_request WHERE assigned_artisan_id = ? AND status IN ('VALIDATED', 'CLOSED')";
            try (PreparedStatement ps = conn.prepareStatement(sqlDone)) {
                ps.setString(1, artisanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) done = rs.getInt(1);
                }
            }

            return new ArtisanStats(incoming, planned, 26 + done, avgRating, reviewCount);
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger les statistiques de l'artisan.", e);
        }
    }

    @Override
    public List<Appointment> findUpcomingAppointments(String artisanId, int limit) {
        String sql = "SELECT ap.id, sr.title, u.full_name AS client_name, " +
                "c.id AS city_id, c.name_fr AS city_name_fr, c.name_ar AS city_name_ar, c.latitude, c.longitude, c.is_active AS city_active, " +
                "ap.starts_at " +
                "FROM appointment ap " +
                "JOIN service_request sr ON ap.request_id = sr.id " +
                "JOIN user u ON sr.client_id = u.id " +
                "JOIN city c ON sr.city_id = c.id " +
                "WHERE ap.artisan_id = ? AND ap.starts_at > NOW() " +
                "ORDER BY ap.starts_at ASC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, artisanId);
            ps.setInt(2, limit);
            List<Appointment> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    City city = new City();
                    city.setId(rs.getString("city_id"));
                    city.setNameFr(rs.getString("city_name_fr"));
                    city.setNameAr(rs.getString("city_name_ar"));
                    city.setLatitude(rs.getBigDecimal("latitude"));
                    city.setLongitude(rs.getBigDecimal("longitude"));
                    city.setActive(rs.getBoolean("city_active"));

                    Timestamp ts = rs.getTimestamp("starts_at");
                    LocalDateTime startsAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                    Appointment app = new Appointment(rs.getString("id"), rs.getString("title"), rs.getString("client_name"), city, startsAt);
                    list.add(app);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger les rendez-vous à venir.", e);
        }
    }

    private Artisan mapArtisanBasic(ResultSet rs) throws SQLException {
        Artisan a = new Artisan();
        a.setId(rs.getString("user_id"));
        a.setDisplayName(rs.getString("display_name"));
        a.setDescription(rs.getString("description"));
        a.setYearsOfExperience(rs.getInt("years_of_experience"));
        a.setVerified("VERIFIED".equalsIgnoreCase(rs.getString("verification_status")));
        a.setAverageRating(rs.getDouble("average_rating"));
        a.setReviewCount(rs.getInt("review_count"));

        Date nextAvail = rs.getDate("next_available_on");
        a.setAvailability(calculateAvailability(nextAvail != null ? nextAvail.toLocalDate() : null));

        BigDecimal minRate = rs.getBigDecimal("indicative_rate_min");
        a.setPriceFrom(minRate != null ? minRate : BigDecimal.valueOf(150));

        // City
        City city = new City();
        city.setId(rs.getString("city_id"));
        city.setNameFr(rs.getString("city_name_fr"));
        city.setNameAr(rs.getString("city_name_ar"));
        city.setLatitude(rs.getBigDecimal("latitude"));
        city.setLongitude(rs.getBigDecimal("longitude"));
        city.setActive(rs.getBoolean("city_active"));
        a.setCity(city);

        // Trade
        String tradeId = rs.getString("trade_id");
        if (tradeId != null) {
            Trade trade = new Trade(tradeId, rs.getString("trade_name_fr"), rs.getString("trade_name_ar"), rs.getBoolean("trade_active"));
            a.setTrade(trade);
        }

        return a;
    }

    private List<String> loadCoveredAreas(Connection conn, String artisanId) throws SQLException {
        String sql = "SELECT c.name_fr FROM artisan_coverage ac JOIN city c ON ac.city_id = c.id WHERE ac.artisan_id = ?";
        List<String> areas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, artisanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    areas.add(rs.getString("name_fr"));
                }
            }
        }
        return areas;
    }

    private List<Review> loadReviews(Connection conn, String artisanId) throws SQLException {
        String sql = "SELECT u.full_name, r.rating, r.comment, r.published_on FROM review r JOIN user u ON r.author_id = u.id WHERE r.artisan_id = ? ORDER BY r.published_on DESC LIMIT 5";
        List<Review> reviews = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, artisanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date pubDate = rs.getDate("published_on");
                    LocalDate d = pubDate != null ? pubDate.toLocalDate() : LocalDate.now();
                    reviews.add(new Review(rs.getString("full_name"), rs.getInt("rating"), rs.getString("comment"), d));
                }
            }
        }
        return reviews;
    }

    private Availability calculateAvailability(LocalDate date) {
        if (date == null) return Availability.LATER;
        LocalDate today = LocalDate.now();
        if (date.isEqual(today)) return Availability.TODAY;
        if (date.isEqual(today.plusDays(1))) return Availability.TOMORROW;
        if (!date.isAfter(today.plusDays(7))) return Availability.THIS_WEEK;
        return Availability.LATER;
    }
}
