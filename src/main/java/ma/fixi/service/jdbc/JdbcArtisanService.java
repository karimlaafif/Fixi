package ma.fixi.service.jdbc;

import ma.fixi.dto.ArtisanSearchCriteria;
import ma.fixi.model.*;
import ma.fixi.service.ArtisanService;
import ma.fixi.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcArtisanService implements ArtisanService {

    private static final String BASE_QUERY = """
        SELECT DISTINCT
            ap.user_id AS artisan_id,
            ap.display_name,
            ap.description,
            ap.city_id,
            c.name_fr AS city_name,
            c.name_ar AS city_ar,
            c.latitude,
            c.longitude,
            ap.years_of_experience,
            ap.verification_status,
            ap.average_rating,
            ap.review_count,
            at.indicative_rate_min,
            ap.next_available_on
        FROM artisan_profile ap
        JOIN city c ON c.id = ap.city_id
        LEFT JOIN artisan_trade at ON at.artisan_id = ap.user_id
        LEFT JOIN trade t ON t.id = at.trade_id
        """;

    @Override
    public List<Artisan> search(ArtisanSearchCriteria criteria) {
        List<Artisan> artisans = new ArrayList<>();

        StringBuilder sql = new StringBuilder(BASE_QUERY);
        sql.append(" WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (criteria.text() != null && !criteria.text().isBlank()) {
            sql.append("""
                AND (
                    ap.display_name LIKE ?
                    OR t.name_fr LIKE ?
                    OR t.name_ar LIKE ?
                )
                """);
            String text = "%" + criteria.text().trim() + "%";
            params.add(text);
            params.add(text);
            params.add(text);
        }

        if (criteria.trade() != null) {
            sql.append(" AND at.trade_id = ? ");
            params.add(criteria.trade().getId());
        }

        if (criteria.city() != null) {
            sql.append("""
                AND (
                    ap.city_id = ?
                    OR EXISTS (
                        SELECT 1 FROM artisan_coverage ac
                        WHERE ac.artisan_id = ap.user_id
                        AND ac.city_id = ?
                    )
                )
                """);
            params.add(criteria.city().getId());
            params.add(criteria.city().getId());
        }

        if (criteria.minRating() > 0) {
            sql.append(" AND ap.average_rating >= ? ");
            params.add(criteria.minRating());
        }

        if (criteria.verifiedOnly()) {
            sql.append(" AND ap.verification_status = 'VERIFIED' ");
        }

        if (criteria.availableBy() != null) {
            sql.append(" AND ap.next_available_on <= ? ");
            params.add(availabilityDate(criteria.availableBy()));
        }

        ArtisanSearchCriteria.Sort sort = criteria.sort();

        if (sort == null) {
            sort = ArtisanSearchCriteria.Sort.RATING;
        }

        switch (sort) {
            case PRICE ->
                sql.append(" ORDER BY at.indicative_rate_min ASC ");

            case EXPERIENCE ->
                sql.append(" ORDER BY ap.years_of_experience DESC ");

            default ->
                sql.append(" ORDER BY ap.average_rating DESC ");
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            setParameters(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    artisans.add(mapArtisan(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                "Erreur lors de la recherche des artisans", e);
        }

        return artisans;
    }

    @Override
    public Optional<Artisan> findById(String artisanId) {

        String sql = BASE_QUERY + """
            WHERE ap.user_id = ?
            LIMIT 1
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, artisanId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                Artisan artisan = mapArtisan(rs);

                artisan.setCoveredAreas(
                    loadCoveredAreas(con, artisanId));

                artisan.setReviews(
                    loadReviews(con, artisanId));

                return Optional.of(artisan);
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                "Erreur lors du chargement de l'artisan", e);
        }
    }

    @Override
    public List<Artisan> findTopRated(int limit) {

        List<Artisan> artisans = new ArrayList<>();

        String sql = BASE_QUERY + """
            WHERE ap.verification_status = 'VERIFIED'
            ORDER BY ap.average_rating DESC
            LIMIT ?
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, Math.max(1, limit));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    artisans.add(mapArtisan(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                "Erreur lors du chargement des meilleurs artisans", e);
        }

        return artisans;
    }

    @Override
    public ArtisanStats getStats(String artisanId) {

        try (Connection con = DBConnection.getConnection()) {

            int newRequests = count(con, """
                SELECT COUNT(*)
                FROM request_recipient
                WHERE artisan_id = ?
                AND response_status = 'PENDING'
                """, artisanId);

            int planned = count(con, """
                SELECT COUNT(*)
                FROM appointment
                WHERE artisan_id = ?
                AND starts_at > NOW()
                """, artisanId);

            int completed = count(con, """
                SELECT COUNT(*)
                FROM service_request
                WHERE assigned_artisan_id = ?
                AND status = 'VALIDATED'
                """, artisanId);

            double rating = 0;
            int reviewCount = 0;

            String sql = """
                SELECT average_rating, review_count
                FROM artisan_profile
                WHERE user_id = ?
                """;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, artisanId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        rating = rs.getDouble("average_rating");
                        reviewCount = rs.getInt("review_count");
                    }
                }
            }

            return new ArtisanStats(
                newRequests,
                planned,
                completed,
                rating,
                reviewCount
            );

        } catch (SQLException e) {
            throw new IllegalStateException(
                "Erreur lors du chargement des statistiques", e);
        }
    }

    @Override
    public List<Appointment> findUpcomingAppointments(
            String artisanId, int limit) {

        List<Appointment> appointments = new ArrayList<>();

        String sql = """
            SELECT
                a.id,
                a.intervention_id,
                a.title,
                u.full_name AS client_name,
                c.id AS city_id,
                c.name_fr AS city_name,
                c.name_ar AS city_ar,
                c.latitude,
                c.longitude,
                a.starts_at
            FROM appointment a
            JOIN user u ON u.id = a.client_id
            JOIN city c ON c.id = a.city_id
            WHERE a.artisan_id = ?
            AND a.starts_at > NOW()
            ORDER BY a.starts_at ASC
            LIMIT ?
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, artisanId);
            ps.setInt(2, Math.max(1, limit));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    City city = mapCity(rs);

                    appointments.add(new Appointment(
                        rs.getString("id"),
                        rs.getString("intervention_id"),
                        rs.getString("title"),
                        rs.getString("client_name"),
                        city,
                        rs.getTimestamp("starts_at").toLocalDateTime()
                    ));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                "Erreur lors du chargement des rendez-vous", e);
        }

        return appointments;
    }

    private Artisan mapArtisan(ResultSet rs) throws SQLException {

        Artisan artisan = new Artisan();

        artisan.setId(rs.getString("artisan_id"));
        artisan.setDisplayName(rs.getString("display_name"));
        artisan.setDescription(rs.getString("description"));
        artisan.setCity(mapCity(rs));

        artisan.setYearsOfExperience(
            rs.getInt("years_of_experience"));

        artisan.setVerified(
            "VERIFIED".equals(rs.getString("verification_status")));

        artisan.setAverageRating(
            rs.getDouble("average_rating"));

        artisan.setReviewCount(
            rs.getInt("review_count"));

        BigDecimal price = rs.getBigDecimal("indicative_rate_min");
        artisan.setPriceFrom(price);

        Date nextDate = rs.getDate("next_available_on");

        artisan.setAvailability(
            calculateAvailability(nextDate));

        return artisan;
    }

    private City mapCity(ResultSet rs) throws SQLException {

        return new City(
            rs.getString("city_id"),
            rs.getString("city_name"),
            rs.getString("city_ar"),
            rs.getDouble("latitude"),
            rs.getDouble("longitude")
        );
    }

    private Availability calculateAvailability(Date date) {

        if (date == null) {
            return Availability.LATER;
        }

        LocalDate today = LocalDate.now();
        LocalDate availableDate = date.toLocalDate();

        if (!availableDate.isAfter(today)) {
            return Availability.TODAY;
        }

        if (availableDate.equals(today.plusDays(1))) {
            return Availability.TOMORROW;
        }

        if (!availableDate.isAfter(today.plusDays(7))) {
            return Availability.THIS_WEEK;
        }

        return Availability.LATER;
    }

    private LocalDate availabilityDate(Availability availability) {

        LocalDate today = LocalDate.now();

        return switch (availability) {
            case TODAY -> today;
            case TOMORROW -> today.plusDays(1);
            case THIS_WEEK -> today.plusDays(7);
            case LATER -> today.plusDays(8);
        };
    }

    private List<String> loadCoveredAreas(
            Connection con, String artisanId) throws SQLException {

        List<String> areas = new ArrayList<>();

        String sql = """
            SELECT c.name_fr
            FROM artisan_coverage ac
            JOIN city c ON c.id = ac.city_id
            WHERE ac.artisan_id = ?
            ORDER BY c.name_fr
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, artisanId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    areas.add(rs.getString("name_fr"));
                }
            }
        }

        return areas;
    }

    private List<Review> loadReviews(
            Connection con, String artisanId) throws SQLException {

        List<Review> reviews = new ArrayList<>();

        String sql = """
            SELECT
                u.full_name AS author_name,
                r.rating,
                r.comment,
                r.published_on
            FROM review r
            JOIN user u ON u.id = r.client_id
            WHERE r.artisan_id = ?
            ORDER BY r.published_on DESC
            LIMIT 5
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, artisanId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    Date date = rs.getDate("published_on");

                    reviews.add(new Review(
                        rs.getString("author_name"),
                        rs.getInt("rating"),
                        rs.getString("comment"),
                        date == null ? null : date.toLocalDate()
                    ));
                }
            }
        }

        return reviews;
    }

    private int count(
            Connection con, String sql, String artisanId)
            throws SQLException {

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, artisanId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void setParameters(
            PreparedStatement ps, List<Object> params)
            throws SQLException {

        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}
