package ma.fixi.service;

import java.util.List;
import ma.fixi.model.Conversation;
import ma.fixi.model.Message;

/** Messagerie client ↔ artisan. Tables : {@code conversation}, {@code message}, {@code message_attachment}. */
public interface MessageService {

    /** Conversations de l'utilisateur, la plus récente en premier. */
    List<Conversation> findConversations(String userId);

    /** Messages d'une conversation, du plus ancien au plus récent. Marque ceux reçus comme lus. */
    List<Message> findMessages(String conversationId, String readerId);

    /**
     * Envoie un message.
     *
     * @param attachmentName nom du fichier joint, ou {@code null}
     */
    Message send(String conversationId, String senderId, String body, String attachmentName);

    /**
     * Renvoie la conversation entre ce client et cet artisan, en la créant si besoin
     * (bouton « Contacter » d'un profil artisan).
     */
    Conversation openWithArtisan(String clientId, String artisanId);
}
