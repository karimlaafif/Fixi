package ma.fixi.service.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import ma.fixi.model.Conversation;
import ma.fixi.model.Message;
import ma.fixi.service.MessageService;
import ma.fixi.service.ServiceException;
import ma.fixi.util.DBConnection;

/** Messagerie privée client ↔ artisan persistée dans MySQL. */
public class JdbcMessageService implements MessageService {

    @Override
    public List<Conversation> findConversations(String userId) {
        String sql = "SELECT c.id, c.request_id, c.client_id, c.artisan_id, " +
                "uClient.full_name AS client_name, " +
                "ap.display_name AS artisan_name, " +
                "t.name_fr AS trade_name " +
                "FROM conversation c " +
                "JOIN user uClient ON c.client_id = uClient.id " +
                "LEFT JOIN artisan_profile ap ON c.artisan_id = ap.user_id " +
                "LEFT JOIN artisan_trade at ON ap.user_id = at.artisan_id " +
                "LEFT JOIN trade t ON at.trade_id = t.id " +
                "WHERE c.client_id = ? OR c.artisan_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);

            List<Conversation> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Conversation conv = new Conversation();
                    conv.setId(rs.getString("id"));
                    conv.setRequestId(rs.getString("request_id"));

                    String clientId = rs.getString("client_id");
                    String artisanId = rs.getString("artisan_id");
                    String clientName = rs.getString("client_name");
                    String artisanName = rs.getString("artisan_name");
                    String tradeName = rs.getString("trade_name");

                    if (userId.equals(clientId)) {
                        conv.setOtherPartyId(artisanId);
                        conv.setArtisanId(artisanId);
                        conv.setOtherPartyName(artisanName != null ? artisanName : "Artisan");
                        conv.setOtherPartySubtitle(tradeName != null ? tradeName : "");
                    } else {
                        conv.setOtherPartyId(clientId);
                        conv.setArtisanId(null);
                        conv.setOtherPartyName(clientName != null ? clientName : "Client");
                        conv.setOtherPartySubtitle("Client");
                    }

                    // Dernier message et non lus
                    fillLastMessageAndUnread(conn, conv, userId);
                    list.add(conv);
                }
            }

            return list.stream()
                    .sorted(Comparator.comparing(Conversation::getLastMessageAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger les conversations.", e);
        }
    }

    @Override
    public List<Message> findMessages(String conversationId, String readerId) {
        String selectSql = "SELECT id, conversation_id, sender_id, body, attachment_name, sent_at, read_at " +
                "FROM message WHERE conversation_id = ? ORDER BY sent_at ASC";

        String updateReadSql = "UPDATE message SET read_at = NOW() WHERE conversation_id = ? AND sender_id <> ? AND read_at IS NULL";

        try (Connection conn = DBConnection.getConnection()) {
            List<Message> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, conversationId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Message m = new Message();
                        m.setId(rs.getString("id"));
                        m.setConversationId(rs.getString("conversation_id"));
                        m.setSenderId(rs.getString("sender_id"));
                        m.setBody(rs.getString("body"));
                        m.setAttachmentName(rs.getString("attachment_name"));

                        Timestamp sent = rs.getTimestamp("sent_at");
                        if (sent != null) m.setSentAt(sent.toLocalDateTime());

                        Timestamp read = rs.getTimestamp("read_at");
                        if (read != null) {
                            m.setReadAt(read.toLocalDateTime());
                        } else if (!m.getSenderId().equals(readerId)) {
                            m.setReadAt(LocalDateTime.now());
                        }

                        list.add(m);
                    }
                }
            }

            // Marquer comme lus
            try (PreparedStatement psUpdate = conn.prepareStatement(updateReadSql)) {
                psUpdate.setString(1, conversationId);
                psUpdate.setString(2, readerId);
                psUpdate.executeUpdate();
            }

            return list;
        } catch (SQLException e) {
            throw new ServiceException("Impossible de charger les messages.", e);
        }
    }

    @Override
    public Message send(String conversationId, String senderId, String body, String attachmentName) {
        if ((body == null || body.isBlank()) && attachmentName == null) {
            throw new ServiceException("Le message est vide.");
        }

        String messageId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        String sql = "INSERT INTO message (id, conversation_id, sender_id, body, attachment_name, sent_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, messageId);
            ps.setString(2, conversationId);
            ps.setString(3, senderId);
            ps.setString(4, body == null ? "" : body.trim());
            ps.setString(5, attachmentName);
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.executeUpdate();

            Message msg = new Message(messageId, conversationId, senderId, body == null ? "" : body.trim(), now);
            msg.setAttachmentName(attachmentName);
            return msg;
        } catch (SQLException e) {
            throw new ServiceException("Impossible d'envoyer le message.", e);
        }
    }

    @Override
    public Conversation openWithArtisan(String clientId, String artisanId) {
        String selectSql = "SELECT id, request_id FROM conversation WHERE client_id = ? AND artisan_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            String convId = null;
            String reqId = null;

            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, clientId);
                ps.setString(2, artisanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        convId = rs.getString("id");
                        reqId = rs.getString("request_id");
                    }
                }
            }

            if (convId == null) {
                convId = UUID.randomUUID().toString();
                String insertSql = "INSERT INTO conversation (id, request_id, client_id, artisan_id, created_at) VALUES (?, NULL, ?, ?, NOW())";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, convId);
                    ps.setString(2, clientId);
                    ps.setString(3, artisanId);
                    ps.executeUpdate();
                }
            }

            // Récupérer les infos de l'artisan
            String artisanName = "Artisan";
            String tradeName = "";
            String artSql = "SELECT ap.display_name, t.name_fr FROM artisan_profile ap " +
                    "LEFT JOIN artisan_trade at ON ap.user_id = at.artisan_id " +
                    "LEFT JOIN trade t ON at.trade_id = t.id WHERE ap.user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(artSql)) {
                ps.setString(1, artisanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        artisanName = rs.getString("display_name");
                        tradeName = rs.getString("name_fr");
                    }
                }
            }

            Conversation conv = new Conversation();
            conv.setId(convId);
            conv.setRequestId(reqId);
            conv.setOtherPartyId(artisanId);
            conv.setArtisanId(artisanId);
            conv.setOtherPartyName(artisanName);
            conv.setOtherPartySubtitle(tradeName != null ? tradeName : "");

            fillLastMessageAndUnread(conn, conv, clientId);
            return conv;
        } catch (SQLException e) {
            throw new ServiceException("Impossible d'ouvrir la conversation.", e);
        }
    }

    private void fillLastMessageAndUnread(Connection conn, Conversation conv, String viewerId) throws SQLException {
        String lastMsgSql = "SELECT body, attachment_name, sent_at FROM message WHERE conversation_id = ? ORDER BY sent_at DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(lastMsgSql)) {
            ps.setString(1, conv.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String body = rs.getString("body");
                    String att = rs.getString("attachment_name");
                    Timestamp sent = rs.getTimestamp("sent_at");
                    if (sent != null) conv.setLastMessageAt(sent.toLocalDateTime());
                    if (att != null && (body == null || body.isBlank())) {
                        conv.setLastMessagePreview("Pièce jointe : " + att);
                    } else {
                        conv.setLastMessagePreview(body);
                    }
                }
            }
        }

        String unreadSql = "SELECT COUNT(*) FROM message WHERE conversation_id = ? AND sender_id <> ? AND read_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(unreadSql)) {
            ps.setString(1, conv.getId());
            ps.setString(2, viewerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) conv.setUnreadCount(rs.getInt(1));
            }
        }
    }
}
