package ma.fixi.service.impl;

import ma.fixi.model.Conversation;
import ma.fixi.model.Message;
import ma.fixi.service.MessageService;
import ma.fixi.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JdbcMessageService implements MessageService {

    @Override
    public List<Conversation> findConversations(String userId) {

        String sql = """
            SELECT
                c.id,
                c.request_id,
                CASE
                    WHEN c.client_id = ? THEN c.artisan_id
                    ELSE c.client_id
                END AS other_party_id,
                CASE
                    WHEN c.client_id = ? THEN artisan.full_name
                    ELSE client.full_name
                END AS other_party_name,
                CASE
                    WHEN c.client_id = ? THEN 'Artisan'
                    ELSE 'Client'
                END AS other_party_subtitle,
                CASE
                    WHEN c.client_id = ? THEN c.artisan_id
                    ELSE NULL
                END AS artisan_id,
                (SELECT m.body
                 FROM message m
                 WHERE m.conversation_id = c.id
                 ORDER BY m.sent_at DESC
                 LIMIT 1) AS last_message_preview,
                (SELECT m.sent_at
                 FROM message m
                 WHERE m.conversation_id = c.id
                 ORDER BY m.sent_at DESC
                 LIMIT 1) AS last_message_at,
                (SELECT COUNT(*)
                 FROM message m
                 WHERE m.conversation_id = c.id
                   AND m.sender_id <> ?
                   AND m.read_at IS NULL) AS unread_count
            FROM conversation c
            JOIN user client ON client.id = c.client_id
            JOIN user artisan ON artisan.id = c.artisan_id
            WHERE c.client_id = ? OR c.artisan_id = ?
            ORDER BY c.updated_at DESC
            """;

        List<Conversation> conversations = new ArrayList<>();

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setString(3, userId);
            ps.setString(4, userId);
            ps.setString(5, userId);
            ps.setString(6, userId);
            ps.setString(7, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Conversation conversation = new Conversation();

                    conversation.setId(rs.getString("id"));
                    conversation.setRequestId(
                            rs.getString("request_id"));
                    conversation.setOtherPartyId(
                            rs.getString("other_party_id"));
                    conversation.setOtherPartyName(
                            rs.getString("other_party_name"));
                    conversation.setOtherPartySubtitle(
                            rs.getString("other_party_subtitle"));
                    conversation.setArtisanId(
                            rs.getString("artisan_id"));
                    conversation.setLastMessagePreview(
                            rs.getString("last_message_preview"));

                    Timestamp lastMessage =
                            rs.getTimestamp("last_message_at");

                    conversation.setLastMessageAt(
                            lastMessage == null
                                    ? null
                                    : lastMessage.toLocalDateTime());

                    conversation.setUnreadCount(
                            rs.getInt("unread_count"));

                    conversations.add(conversation);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erreur lors du chargement des conversations.", e);
        }

        return conversations;
    }

    @Override
    public List<Message> findMessages(
            String conversationId,
            String readerId) {

        List<Message> messages = new ArrayList<>();

        String checkSql = """
            SELECT id
            FROM conversation
            WHERE id = ?
              AND (client_id = ? OR artisan_id = ?)
            """;

        String updateSql = """
            UPDATE message
            SET read_at = NOW()
            WHERE conversation_id = ?
              AND sender_id <> ?
              AND read_at IS NULL
            """;

        String selectSql = """
            SELECT id, conversation_id, sender_id, body,
                   sent_at, read_at, attachment_name
            FROM message
            WHERE conversation_id = ?
            ORDER BY sent_at ASC
            """;

        try (Connection cn = DBConnection.getConnection()) {

            // Vérifier que l'utilisateur appartient à la conversation.
            try (PreparedStatement ps = cn.prepareStatement(checkSql)) {

                ps.setString(1, conversationId);
                ps.setString(2, readerId);
                ps.setString(3, readerId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException(
                                "Conversation introuvable ou accès refusé.");
                    }
                }
            }

            // Marquer les messages reçus comme lus.
            try (PreparedStatement ps = cn.prepareStatement(updateSql)) {
                ps.setString(1, conversationId);
                ps.setString(2, readerId);
                ps.executeUpdate();
            }

            // Récupérer les messages.
            try (PreparedStatement ps = cn.prepareStatement(selectSql)) {

                ps.setString(1, conversationId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        Message message = new Message();

                        message.setId(rs.getString("id"));
                        message.setConversationId(
                                rs.getString("conversation_id"));
                        message.setSenderId(
                                rs.getString("sender_id"));
                        message.setBody(rs.getString("body"));

                        Timestamp sentAt = rs.getTimestamp("sent_at");
                        if (sentAt != null) {
                            message.setSentAt(
                                    sentAt.toLocalDateTime());
                        }

                        Timestamp readAt = rs.getTimestamp("read_at");
                        if (readAt != null) {
                            message.setReadAt(
                                    readAt.toLocalDateTime());
                        }

                        message.setAttachmentName(
                                rs.getString("attachment_name"));

                        messages.add(message);
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erreur lors du chargement des messages.", e);
        }

        return messages;
    }

    @Override
    public Message send(
            String conversationId,
            String senderId,
            String body,
            String attachmentName) {

        if ((body == null || body.isBlank())
                && (attachmentName == null
                || attachmentName.isBlank())) {
            throw new IllegalArgumentException(
                    "Le message ne peut pas être vide.");
        }

        String checkSql = """
            SELECT id
            FROM conversation
            WHERE id = ?
              AND (client_id = ? OR artisan_id = ?)
            """;

        String insertSql = """
            INSERT INTO message
                (id, conversation_id, sender_id, body,
                 sent_at, attachment_name)
            VALUES (?, ?, ?, ?, NOW(), ?)
            """;

        String updateSql = """
            UPDATE conversation
            SET updated_at = NOW()
            WHERE id = ?
            """;

        try (Connection cn = DBConnection.getConnection()) {

            cn.setAutoCommit(false);

            try {
                // Vérifier les droits de l'expéditeur.
                try (PreparedStatement ps =
                             cn.prepareStatement(checkSql)) {

                    ps.setString(1, conversationId);
                    ps.setString(2, senderId);
                    ps.setString(3, senderId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Utilisateur non autorisé.");
                        }
                    }
                }

                String messageId = UUID.randomUUID().toString();

                try (PreparedStatement ps =
                             cn.prepareStatement(insertSql)) {

                    ps.setString(1, messageId);
                    ps.setString(2, conversationId);
                    ps.setString(3, senderId);
                    ps.setString(4, body == null ? "" : body);
                    ps.setString(5, attachmentName);

                    ps.executeUpdate();
                }

                try (PreparedStatement ps =
                             cn.prepareStatement(updateSql)) {

                    ps.setString(1, conversationId);
                    ps.executeUpdate();
                }

                cn.commit();

                Message message = new Message();
                message.setId(messageId);
                message.setConversationId(conversationId);
                message.setSenderId(senderId);
                message.setBody(body == null ? "" : body);
                message.setSentAt(LocalDateTime.now());
                message.setAttachmentName(attachmentName);

                return message;

            } catch (Exception e) {
                cn.rollback();
                throw e;

            } finally {
                cn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erreur lors de l'envoi du message.", e);
        }
    }

    @Override
    public Conversation openWithArtisan(
            String clientId,
            String artisanId) {

        String insertSql = """
            INSERT INTO conversation
                (id, client_id, artisan_id)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE id = id
            """;

        String selectSql = """
            SELECT
                c.id,
                c.request_id,
                c.artisan_id,
                artisan.full_name AS artisan_name,
                (SELECT m.body
                 FROM message m
                 WHERE m.conversation_id = c.id
                 ORDER BY m.sent_at DESC
                 LIMIT 1) AS last_message_preview,
                (SELECT m.sent_at
                 FROM message m
                 WHERE m.conversation_id = c.id
                 ORDER BY m.sent_at DESC
                 LIMIT 1) AS last_message_at,
                (SELECT COUNT(*)
                 FROM message m
                 WHERE m.conversation_id = c.id
                   AND m.sender_id <> ?
                   AND m.read_at IS NULL) AS unread_count
            FROM conversation c
            JOIN user artisan ON artisan.id = c.artisan_id
            WHERE c.client_id = ?
              AND c.artisan_id = ?
            """;

        try (Connection cn = DBConnection.getConnection()) {

            // Créer la conversation si elle n'existe pas.
            try (PreparedStatement ps =
                         cn.prepareStatement(insertSql)) {

                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, clientId);
                ps.setString(3, artisanId);

                ps.executeUpdate();
            }

            // Récupérer la conversation.
            try (PreparedStatement ps =
                         cn.prepareStatement(selectSql)) {

                ps.setString(1, clientId);
                ps.setString(2, clientId);
                ps.setString(3, artisanId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        Conversation conversation =
                                new Conversation();

                        conversation.setId(rs.getString("id"));
                        conversation.setRequestId(
                                rs.getString("request_id"));
                        conversation.setOtherPartyId(artisanId);
                        conversation.setOtherPartyName(
                                rs.getString("artisan_name"));
                        conversation.setOtherPartySubtitle("Artisan");
                        conversation.setArtisanId(artisanId);
                        conversation.setLastMessagePreview(
                                rs.getString("last_message_preview"));

                        Timestamp lastMessage =
                                rs.getTimestamp("last_message_at");

                        conversation.setLastMessageAt(
                                lastMessage == null
                                        ? null
                                        : lastMessage.toLocalDateTime());

                        conversation.setUnreadCount(
                                rs.getInt("unread_count"));

                        return conversation;
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erreur lors de l'ouverture de la conversation.", e);
        }

        throw new IllegalStateException(
                "Impossible de créer ou récupérer la conversation.");
    }
}
