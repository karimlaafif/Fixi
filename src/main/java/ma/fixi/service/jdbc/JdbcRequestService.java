package ma.fixi.service.jdbc;

import ma.fixi.dto.NewRequestForm;
import ma.fixi.model.City;
import ma.fixi.model.RequestStatus;
import ma.fixi.model.ServiceRequest;
import ma.fixi.model.Trade;
import ma.fixi.model.Urgency;
import ma.fixi.service.RequestService;
import ma.fixi.util.DBConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JdbcRequestService implements RequestService {

    private static final Path UPLOAD_DIR =
            Path.of("storage", "requests");

    @Override
    public List<ServiceRequest> findByClient(String clientId) {
        String sql = """
            SELECT sr.*,
                   t.id AS trade_id, t.name_fr AS trade_name_fr,
                   t.name_ar AS trade_name_ar,
                   c.id AS city_id, c.name_fr AS city_name_fr,
                   c.name_ar AS city_name_ar,
                   c.latitude, c.longitude
            FROM service_request sr
            LEFT JOIN trade t ON t.id = sr.trade_id
            LEFT JOIN city c ON c.id = sr.city_id
            WHERE sr.client_id = ?
            ORDER BY sr.created_at DESC
            """;

        List<ServiceRequest> requests = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, clientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ServiceRequest request = mapRequest(rs, con);
                    requests.add(request);
                }
            }

            return requests;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erreur lors du chargement des demandes", e);
        }
    }

    @Override
    public ServiceRequest create(String clientId, NewRequestForm form) {
        String requestId = UUID.randomUUID().toString();
        String reference = "D-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();

        String sql = """
            INSERT INTO service_request
            (id, reference, client_id, title, description,
             trade_id, city_id, address_line, urgency,
             budget, preferred_date, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SENT', NOW())
            """;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, requestId);
                    ps.setString(2, reference);
                    ps.setString(3, clientId);
                    ps.setString(4, form.title());
                    ps.setString(5, form.description());
                    ps.setString(6, form.trade().getId());
                    ps.setString(7, form.city().getId());
                    ps.setString(8, form.addressLine());
                    ps.setString(9, form.urgency().name());

                    if (form.budget() == null) {
                        ps.setNull(10, Types.DECIMAL);
                    } else {
                        ps.setBigDecimal(10, form.budget());
                    }

                    if (form.preferredDate() == null) {
                        ps.setNull(11, Types.DATE);
                    } else {
                        ps.setDate(11, Date.valueOf(form.preferredDate()));
                    }

                    ps.executeUpdate();
                }

                addRecipients(con, requestId, form);
                saveAttachments(con, requestId, form.photos());
                addStatusEvent(con, requestId, RequestStatus.SENT);

                con.commit();

                return findRequestById(con, requestId);

            } catch (Exception e) {
                con.rollback();
                throw new IllegalStateException(
                        "Impossible de créer la demande", e);
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erreur de base de données", e);
        }
    }

    @Override
    public ServiceRequest cancel(String requestId) {
        return changeStatus(
                requestId,
                RequestStatus.CANCELLED,
                RequestStatus::canBeCancelledByClient
        );
    }

    @Override
    public ServiceRequest changePreferredDate(
            String requestId, LocalDate newDate) {

        String sql = """
            UPDATE service_request
            SET preferred_date = ?
            WHERE id = ?
            """;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                ServiceRequest request = findRequestById(con, requestId);

                if (request == null ||
                    !request.getStatus().canChangeDate()) {
                    throw new IllegalStateException(
                            "La date ne peut pas être modifiée");
                }

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setDate(1, Date.valueOf(newDate));
                    ps.setString(2, requestId);
                    ps.executeUpdate();
                }

                con.commit();
                request.setPreferredDate(newDate);
                return request;

            } catch (Exception e) {
                con.rollback();
                throw new IllegalStateException(e);
            }

        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ServiceRequest confirmCompletion(String requestId) {
        return changeStatus(
                requestId,
                RequestStatus.VALIDATED,
                RequestStatus::canBeValidatedByClient
        );
    }

    @Override
    public List<ServiceRequest> findIncomingForArtisan(String artisanId) {
        String sql = """
            SELECT sr.*,
                   t.id AS trade_id, t.name_fr AS trade_name_fr,
                   t.name_ar AS trade_name_ar,
                   c.id AS city_id, c.name_fr AS city_name_fr,
                   c.name_ar AS city_name_ar,
                   c.latitude, c.longitude
            FROM service_request sr
            JOIN request_recipient rr ON rr.request_id = sr.id
            LEFT JOIN trade t ON t.id = sr.trade_id
            LEFT JOIN city c ON c.id = sr.city_id
            WHERE rr.artisan_id = ?
              AND rr.status = 'PENDING'
            ORDER BY sr.created_at DESC
            """;

        List<ServiceRequest> requests = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, artisanId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapRequest(rs, con));
                }
            }

            return requests;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erreur lors du chargement des demandes reçues", e);
        }
    }

    @Override
    public void decline(String artisanId, String requestId) {
        String sql = """
            UPDATE request_recipient
            SET status = 'DECLINED'
            WHERE request_id = ?
              AND artisan_id = ?
              AND status = 'PENDING'
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, requestId);
            ps.setString(2, artisanId);

            if (ps.executeUpdate() == 0) {
                throw new IllegalStateException(
                        "Demande introuvable ou déjà traitée");
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erreur lors du refus de la demande", e);
        }
    }

    private ServiceRequest changeStatus(
            String requestId,
            RequestStatus newStatus,
            java.util.function.Predicate<RequestStatus> rule) {

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                ServiceRequest request = findRequestById(con, requestId);

                if (request == null || !rule.test(request.getStatus())) {
                    throw new IllegalStateException(
                            "Transition de statut non autorisée");
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE service_request SET status = ? WHERE id = ?")) {
                    ps.setString(1, newStatus.name());
                    ps.setString(2, requestId);
                    ps.executeUpdate();
                }

                addStatusEvent(con, requestId, newStatus);
                con.commit();

                request.setStatus(newStatus);
                return request;

            } catch (Exception e) {
                con.rollback();
                throw new IllegalStateException(e);
            }

        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void addRecipients(
            Connection con, String requestId, NewRequestForm form)
            throws SQLException {

        String sql = """
            INSERT INTO request_recipient
            (id, request_id, artisan_id, status)
            VALUES (?, ?, ?, 'PENDING')
            """;

        List<String> artisanIds = new ArrayList<>();

        if (form.targetArtisanId() != null) {
            artisanIds.add(form.targetArtisanId());
        } else {
            String search = """
                SELECT ap.user_id
                FROM artisan_profile ap
                JOIN artisan_trade at ON at.artisan_id = ap.user_id
                JOIN artisan_coverage ac ON ac.artisan_id = ap.user_id
                WHERE at.trade_id = ?
                  AND ac.city_id = ?
                """;

            try (PreparedStatement ps = con.prepareStatement(search)) {
                ps.setString(1, form.trade().getId());
                ps.setString(2, form.city().getId());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        artisanIds.add(rs.getString(1));
                    }
                }
            }
        }

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (String artisanId : artisanIds) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, requestId);
                ps.setString(3, artisanId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveAttachments(
            Connection con, String requestId, List<java.io.File> photos)
            throws SQLException, IOException {

        if (photos == null || photos.isEmpty()) {
            return;
        }

        Files.createDirectories(UPLOAD_DIR);

        String sql = """
            INSERT INTO request_attachment
            (id, request_id, file_name, file_path)
            VALUES (?, ?, ?, ?)
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (java.io.File photo : photos) {
                if (photo == null || !photo.isFile()) {
                    continue;
                }

                String storedName =
                        UUID.randomUUID() + "_" + photo.getName();

                Path destination = UPLOAD_DIR.resolve(storedName);
                Files.copy(photo.toPath(), destination);

                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, requestId);
                ps.setString(3, photo.getName());
                ps.setString(4, destination.toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void addStatusEvent(
            Connection con, String requestId, RequestStatus status)
            throws SQLException {

        String sql = """
            INSERT INTO status_event
            (id, request_id, status, created_at)
            VALUES (?, ?, ?, NOW())
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, requestId);
            ps.setString(3, status.name());
            ps.executeUpdate();
        }
    }

    private ServiceRequest findRequestById(
            Connection con, String requestId) throws SQLException {

        String sql = """
            SELECT sr.*,
                   t.id AS trade_id, t.name_fr AS trade_name_fr,
                   t.name_ar AS trade_name_ar,
                   c.id AS city_id, c.name_fr AS city_name_fr,
                   c.name_ar AS city_name_ar,
                   c.latitude, c.longitude
            FROM service_request sr
            LEFT JOIN trade t ON t.id = sr.trade_id
            LEFT JOIN city c ON c.id = sr.city_id
            WHERE sr.id = ?
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, requestId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRequest(rs, con);
                }
                return null;
            }
        }
    }

    private ServiceRequest mapRequest(ResultSet rs, Connection con)
            throws SQLException {

        ServiceRequest request = new ServiceRequest();

        request.setId(rs.getString("id"));
        request.setReference(rs.getString("reference"));
        request.setClientId(rs.getString("client_id"));
        request.setTitle(rs.getString("title"));
        request.setDescription(rs.getString("description"));
        request.setAddressLine(rs.getString("address_line"));
        request.setUrgency(Urgency.valueOf(rs.getString("urgency")));
        request.setBudget(rs.getBigDecimal("budget"));

        Date preferredDate = rs.getDate("preferred_date");
        if (preferredDate != null) {
            request.setPreferredDate(preferredDate.toLocalDate());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            request.setCreatedAt(createdAt.toLocalDateTime());
        }

        request.setStatus(
                RequestStatus.valueOf(rs.getString("status")));

        String tradeId = rs.getString("trade_id");
        if (tradeId != null) {
            request.setTrade(new Trade(
                    tradeId,
                    rs.getString("trade_name_fr"),
                    rs.getString("trade_name_ar"),
                    true));
        }

        String cityId = rs.getString("city_id");
        if (cityId != null) {
            request.setCity(new City(
                    cityId,
                    rs.getString("city_name_fr"),
                    rs.getString("city_name_ar"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")));
        }

        request.setAttachmentNames(loadAttachmentNames(con, request.getId()));

        return request;
    }

    private List<String> loadAttachmentNames(
            Connection con, String requestId) throws SQLException {

        List<String> names = new ArrayList<>();

        String sql = """
            SELECT file_name
            FROM request_attachment
            WHERE request_id = ?
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, requestId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("file_name"));
                }
            }
        }

        return names;
    }
}
