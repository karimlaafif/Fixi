package ma.fixi.service.jdbc;

import java.io.File;
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
import java.util.List;
import java.util.UUID;
import ma.fixi.dto.NewRequestForm;
import ma.fixi.model.Artisan;
import ma.fixi.model.City;
import ma.fixi.model.RequestStatus;
import ma.fixi.model.ServiceRequest;
import ma.fixi.model.Trade;
import ma.fixi.model.Urgency;
import ma.fixi.service.RequestService;
import ma.fixi.service.ServiceException;
import ma.fixi.util.DBConnection;

/** Gestion des demandes de service dans MySQL. */
public class JdbcRequestService implements RequestService {

    @Override
    public List<ServiceRequest> findByClient(String clientId) {
        String sql = baseRequestQuery() + "WHERE sr.client_id = ? ORDER BY sr.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clientId);
            return executeRequestList(conn, ps);
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger les demandes du client.", e);
        }
    }

    @Override
    public ServiceRequest create(String clientId, NewRequestForm form) {
        if (form.title() == null || form.title().isBlank()) {
            throw new ServiceException("Le titre de la demande est obligatoire.");
        }
        if (form.trade() == null || form.trade().getId() == null) {
            throw new ServiceException("Le métier est obligatoire.");
        }
        if (form.city() == null || form.city().getId() == null) {
            throw new ServiceException("La ville est obligatoire.");
        }

        String requestId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String reference = generateNextReference(conn);

                String insertSql = "INSERT INTO service_request (id, reference, client_id, trade_id, city_id, title, " +
                        "description, address_line, urgency, budget, preferred_date, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SENT', ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, requestId);
                    ps.setString(2, reference);
                    ps.setString(3, clientId);
                    ps.setString(4, form.trade().getId());
                    ps.setString(5, form.city().getId());
                    ps.setString(6, form.title().trim());
                    ps.setString(7, form.description() == null ? "" : form.description().trim());
                    ps.setString(8, form.addressLine());
                    ps.setString(9, form.urgency() != null ? form.urgency().name() : Urgency.NORMAL.name());
                    ps.setBigDecimal(10, form.budget());
                    ps.setDate(11, form.preferredDate() != null ? Date.valueOf(form.preferredDate()) : Date.valueOf(LocalDate.now()));
                    ps.setTimestamp(12, Timestamp.valueOf(now));
                    ps.executeUpdate();
                }

                // Destinataires
                if (form.targetArtisanId() != null && !form.targetArtisanId().isBlank()) {
                    insertRecipient(conn, requestId, form.targetArtisanId());
                } else {
                    List<String> artisans = findMatchingArtisans(conn, form.trade().getId(), form.city().getId());
                    for (String artId : artisans) {
                        insertRecipient(conn, requestId, artId);
                    }
                }

                // Pièces jointes
                if (form.photos() != null && !form.photos().isEmpty()) {
                    String attSql = "INSERT INTO request_attachment (request_id, file_path, file_name, sort_order) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement psAtt = conn.prepareStatement(attSql)) {
                        int order = 1;
                        for (File f : form.photos()) {
                            psAtt.setString(1, requestId);
                            psAtt.setString(2, f.getAbsolutePath());
                            psAtt.setString(3, f.getName());
                            psAtt.setInt(4, order++);
                            psAtt.addBatch();
                        }
                        psAtt.executeBatch();
                    }
                }

                // Événement d'historique
                String eventSql = "INSERT INTO status_event (id, request_id, actor_id, from_status, to_status, occurred_at) VALUES (?, ?, ?, NULL, 'SENT', ?)";
                try (PreparedStatement psEv = conn.prepareStatement(eventSql)) {
                    psEv.setString(1, UUID.randomUUID().toString());
                    psEv.setString(2, requestId);
                    psEv.setString(3, clientId);
                    psEv.setTimestamp(4, Timestamp.valueOf(now));
                    psEv.executeUpdate();
                }

                conn.commit();

                // Charger le nom du client
                String clientName = "";
                try (PreparedStatement psUser = conn.prepareStatement("SELECT full_name FROM user WHERE id = ?")) {
                    psUser.setString(1, clientId);
                    try (ResultSet rsUser = psUser.executeQuery()) {
                        if (rsUser.next()) clientName = rsUser.getString("full_name");
                    }
                }

                ServiceRequest req = new ServiceRequest();
                req.setId(requestId);
                req.setReference(reference);
                req.setClientId(clientId);
                req.setClientName(clientName);
                req.setTitle(form.title().trim());
                req.setTrade(form.trade());
                req.setCity(form.city());
                req.setDescription(form.description() == null ? "" : form.description().trim());
                req.setAddressLine(form.addressLine());
                req.setBudget(form.budget());
                req.setPreferredDate(form.preferredDate());
                req.setUrgency(form.urgency() != null ? form.urgency() : Urgency.NORMAL);
                req.setStatus(RequestStatus.SENT);
                req.setCreatedAt(now);

                List<String> names = new ArrayList<>();
                if (form.photos() != null) {
                    for (File f : form.photos()) names.add(f.getName());
                }
                req.setAttachmentNames(names);

                return req;
            } catch (SQLException e) {
                conn.rollback();
                throw new ServiceException("Impossible de créer la demande de service.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur de connexion à la base de données.", e);
        }
    }

    @Override
    public ServiceRequest cancel(String requestId) {
        ServiceRequest r = findById(requestId);
        if (!r.getStatus().canBeCancelledByClient()) {
            throw new ServiceException("Cette demande ne peut plus être annulée : les travaux ont commencé.");
        }
        updateStatus(requestId, RequestStatus.CANCELLED);
        r.setStatus(RequestStatus.CANCELLED);
        return r;
    }

    @Override
    public ServiceRequest changePreferredDate(String requestId, LocalDate newDate) {
        ServiceRequest r = findById(requestId);
        if (!r.getStatus().canChangeDate()) {
            throw new ServiceException("La date ne peut plus être modifiée pour cette demande.");
        }
        if (newDate.isBefore(LocalDate.now())) {
            throw new ServiceException("Choisissez une date à partir d'aujourd'hui.");
        }

        String sql = "UPDATE service_request SET preferred_date = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(newDate));
            ps.setString(2, requestId);
            ps.executeUpdate();
            r.setPreferredDate(newDate);
            return r;
        } catch (SQLException e) {
            throw new ServiceException("Impossible de mettre à jour la date souhaitée.", e);
        }
    }

    @Override
    public ServiceRequest confirmCompletion(String requestId) {
        ServiceRequest r = findById(requestId);
        if (!r.getStatus().canBeValidatedByClient()) {
            throw new ServiceException("L'artisan n'a pas encore déclaré les travaux terminés.");
        }
        updateStatus(requestId, RequestStatus.VALIDATED);
        r.setStatus(RequestStatus.VALIDATED);
        return r;
    }

    @Override
    public List<ServiceRequest> findIncomingForArtisan(String artisanId) {
        String sql = baseRequestQuery() +
                "JOIN request_recipient rr ON sr.id = rr.request_id " +
                "WHERE rr.artisan_id = ? AND rr.response_status = 'PENDING' " +
                "ORDER BY sr.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, artisanId);
            return executeRequestList(conn, ps);
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger les demandes entrantes.", e);
        }
    }

    @Override
    public void decline(String artisanId, String requestId) {
        String sql = "UPDATE request_recipient SET response_status = 'DECLINED' WHERE artisan_id = ? AND request_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, artisanId);
            ps.setString(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ServiceException("Impossible de décliner la demande.", e);
        }
    }

    private ServiceRequest findById(String requestId) {
        String sql = baseRequestQuery() + "WHERE sr.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestId);
            List<ServiceRequest> list = executeRequestList(conn, ps);
            if (list.isEmpty()) {
                throw new ServiceException("Demande introuvable.");
            }
            return list.get(0);
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger la demande.", e);
        }
    }

    private void updateStatus(String requestId, RequestStatus newStatus) {
        String sql = "UPDATE service_request SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ServiceException("Impossible de changer le statut de la demande.", e);
        }
    }

    private String baseRequestQuery() {
        return "SELECT sr.id, sr.reference, sr.client_id, u.full_name AS client_name, sr.title, sr.description, " +
                "sr.address_line, sr.urgency, sr.budget, sr.preferred_date, sr.status, sr.created_at, " +
                "sr.assigned_artisan_id, ap.display_name AS assigned_artisan_name, sr.accepted_quote_total, " +
                "t.id AS trade_id, t.name_fr AS trade_name_fr, t.name_ar AS trade_name_ar, t.is_active AS trade_active, " +
                "c.id AS city_id, c.name_fr AS city_name_fr, c.name_ar AS city_name_ar, c.latitude, c.longitude, c.is_active AS city_active " +
                "FROM service_request sr " +
                "JOIN user u ON sr.client_id = u.id " +
                "JOIN trade t ON sr.trade_id = t.id " +
                "JOIN city c ON sr.city_id = c.id " +
                "LEFT JOIN artisan_profile ap ON sr.assigned_artisan_id = ap.user_id ";
    }

    private List<ServiceRequest> executeRequestList(Connection conn, PreparedStatement ps) throws SQLException {
        List<ServiceRequest> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ServiceRequest req = new ServiceRequest();
                req.setId(rs.getString("id"));
                req.setReference(rs.getString("reference"));
                req.setClientId(rs.getString("client_id"));
                req.setClientName(rs.getString("client_name"));
                req.setTitle(rs.getString("title"));
                req.setDescription(rs.getString("description"));
                req.setAddressLine(rs.getString("address_line"));

                String urg = rs.getString("urgency");
                req.setUrgency(urg != null ? Urgency.valueOf(urg) : Urgency.NORMAL);
                req.setBudget(rs.getBigDecimal("budget"));

                Date prefDate = rs.getDate("preferred_date");
                if (prefDate != null) req.setPreferredDate(prefDate.toLocalDate());

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) req.setCreatedAt(createdAt.toLocalDateTime());

                String st = rs.getString("status");
                req.setStatus(st != null ? RequestStatus.valueOf(st) : RequestStatus.SENT);

                // Trade
                Trade trade = new Trade(rs.getString("trade_id"), rs.getString("trade_name_fr"),
                        rs.getString("trade_name_ar"), rs.getBoolean("trade_active"));
                req.setTrade(trade);

                // City
                City city = new City();
                city.setId(rs.getString("city_id"));
                city.setNameFr(rs.getString("city_name_fr"));
                city.setNameAr(rs.getString("city_name_ar"));
                city.setLatitude(rs.getBigDecimal("latitude"));
                city.setLongitude(rs.getBigDecimal("longitude"));
                city.setActive(rs.getBoolean("city_active"));
                req.setCity(city);

                // Assigned artisan
                String assignedId = rs.getString("assigned_artisan_id");
                if (assignedId != null) {
                    Artisan a = new Artisan();
                    a.setId(assignedId);
                    a.setDisplayName(rs.getString("assigned_artisan_name"));
                    req.setAssignedArtisan(a);
                    req.setAcceptedQuoteTotal(rs.getBigDecimal("accepted_quote_total"));
                }

                // Attachments
                req.setAttachmentNames(loadAttachments(conn, req.getId()));

                list.add(req);
            }
        }
        return list;
    }

    private List<String> loadAttachments(Connection conn, String requestId) {
        List<String> names = new ArrayList<>();
        String sql = "SELECT file_name FROM request_attachment WHERE request_id = ? ORDER BY sort_order";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("file_name"));
                }
            }
        } catch (SQLException ignored) {
        }
        return names;
    }

    private String generateNextReference(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(reference, 3) AS UNSIGNED)), 4586) + 1 FROM service_request WHERE reference LIKE 'D-%'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return "D-" + rs.getInt(1);
            }
        }
        return "D-4587";
    }

    private void insertRecipient(Connection conn, String requestId, String artisanId) throws SQLException {
        String sql = "INSERT INTO request_recipient (request_id, artisan_id, sent_at, response_status) VALUES (?, ?, NOW(), 'PENDING') " +
                "ON DUPLICATE KEY UPDATE response_status = 'PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestId);
            ps.setString(2, artisanId);
            ps.executeUpdate();
        }
    }

    private List<String> findMatchingArtisans(Connection conn, String tradeId, String cityId) throws SQLException {
        String sql = "SELECT DISTINCT ap.user_id FROM artisan_profile ap " +
                "LEFT JOIN artisan_trade at ON ap.user_id = at.artisan_id " +
                "LEFT JOIN artisan_coverage ac ON ap.user_id = ac.artisan_id " +
                "WHERE (at.trade_id = ? OR ? IS NULL) AND (ap.city_id = ? OR ac.city_id = ?)";
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tradeId);
            ps.setString(2, tradeId);
            ps.setString(3, cityId);
            ps.setString(4, cityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("user_id"));
                }
            }
        }
        return list;
    }
}
