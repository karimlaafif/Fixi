package ma.fixi.service.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ma.fixi.model.Trade;
import ma.fixi.service.ServiceException;
import ma.fixi.service.TradeService;
import ma.fixi.util.DBConnection;

/** Métiers stockés dans la base de données relationnelle (table {@code trade}). */
public class JdbcTradeService implements TradeService {

    private static final String SELECT = "SELECT id, name_fr, name_ar, is_active FROM trade ";

    @Override
    public List<Trade> findAll() {
        return query(SELECT + "ORDER BY name_fr");
    }

    @Override
    public List<Trade> findActive() {
        return query(SELECT + "WHERE is_active = TRUE ORDER BY name_fr");
    }

    @Override
    public Trade create(Trade trade) {
        if (trade.getNameFr() == null || trade.getNameFr().isBlank()) {
            throw new ServiceException("Le nom français du métier est obligatoire.");
        }
        ensureUniqueName(trade);
        String id = (trade.getId() == null || trade.getId().isBlank()) ? UUID.randomUUID().toString() : trade.getId();
        String sql = "INSERT INTO trade (id, name_fr, name_ar, is_active) VALUES (?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, trade.getNameFr().trim());
            ps.setString(3, trade.getNameAr() == null ? "" : trade.getNameAr().trim());
            ps.setBoolean(4, trade.isActive());
            ps.executeUpdate();
            return new Trade(id, trade.getNameFr().trim(), trade.getNameAr() == null ? "" : trade.getNameAr().trim(), trade.isActive());
        } catch (SQLException e) {
            throw new ServiceException("Impossible d'enregistrer le métier.", e);
        }
    }

    @Override
    public Trade update(Trade trade) {
        if (trade.getNameFr() == null || trade.getNameFr().isBlank()) {
            throw new ServiceException("Le nom français du métier est obligatoire.");
        }
        ensureUniqueName(trade);
        String sql = "UPDATE trade SET name_fr = ?, name_ar = ?, is_active = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, trade.getNameFr().trim());
            ps.setString(2, trade.getNameAr() == null ? "" : trade.getNameAr().trim());
            ps.setBoolean(3, trade.isActive());
            ps.setString(4, trade.getId());
            if (ps.executeUpdate() == 0) {
                throw new ServiceException("Ce métier n'existe plus. Actualisez la liste.");
            }
            return trade;
        } catch (SQLException e) {
            throw new ServiceException("Impossible de modifier le métier.", e);
        }
    }

    @Override
    public void delete(String tradeId) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM trade WHERE id = ?")) {
            ps.setString(1, tradeId);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ServiceException("Ce métier est utilisé par des artisans ou des demandes. Désactivez-le plutôt.", e);
        } catch (SQLException e) {
            // PostgreSQL foreign key violation error code 23503
            if ("23503".equals(e.getSQLState())) {
                throw new ServiceException("Ce métier est utilisé par des artisans ou des demandes. Désactivez-le plutôt.", e);
            }
            throw new ServiceException("Impossible de supprimer le métier.", e);
        }
    }

    private void ensureUniqueName(Trade trade) {
        String sql = "SELECT COUNT(*) FROM trade WHERE LOWER(name_fr) = LOWER(?) AND id <> ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, trade.getNameFr().trim());
            ps.setString(2, trade.getId() == null ? "" : trade.getId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    throw new ServiceException("Un métier porte déjà ce nom.");
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Impossible de vérifier le nom du métier.", e);
        }
    }

    private List<Trade> query(String sql) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Trade> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Trade(rs.getString("id"), rs.getString("name_fr"),
                        rs.getString("name_ar"), rs.getBoolean("is_active")));
            }
            return list;
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger les métiers. Vérifiez la connexion à la base.", e);
        }
    }
}
