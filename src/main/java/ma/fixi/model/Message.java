package ma.fixi.model;

import java.time.LocalDateTime;

/** Message d'une conversation. Correspond aux tables {@code message} et {@code message_attachment}. */
public class Message {

    private String id;
    private String conversationId;
    private String senderId;
    private String body;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    /** Nom du fichier joint, ou {@code null}. */
    private String attachmentName;

    public Message() {
    }

    public Message(String id, String conversationId, String senderId, String body, LocalDateTime sentAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.body = body;
        this.sentAt = sentAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }
}
