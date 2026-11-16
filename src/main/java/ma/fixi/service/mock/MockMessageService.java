package ma.fixi.service.mock;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import ma.fixi.model.Artisan;
import ma.fixi.model.Conversation;
import ma.fixi.model.Message;
import ma.fixi.model.User;
import ma.fixi.service.MessageService;
import ma.fixi.service.ServiceException;
import ma.fixi.service.mock.MockStore.ConversationRecord;

/** Messagerie fictive en mémoire (pas de temps réel). */
public class MockMessageService implements MessageService {

    private final MockStore store;

    public MockMessageService(MockStore store) {
        this.store = store;
    }

    @Override
    public synchronized List<Conversation> findConversations(String userId) {
        return store.conversations.stream()
                .filter(c -> c.clientId().equals(userId) || c.artisanId().equals(userId))
                .map(c -> toView(c, userId))
                .sorted(Comparator.comparing(Conversation::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public synchronized List<Message> findMessages(String conversationId, String readerId) {
        List<Message> list = store.messages.stream()
                .filter(m -> m.getConversationId().equals(conversationId))
                .sorted(Comparator.comparing(Message::getSentAt))
                .toList();
        list.stream()
                .filter(m -> !m.getSenderId().equals(readerId) && m.getReadAt() == null)
                .forEach(m -> m.setReadAt(LocalDateTime.now()));
        return list;
    }

    @Override
    public synchronized Message send(String conversationId, String senderId, String body, String attachmentName) {
        if ((body == null || body.isBlank()) && attachmentName == null) {
            throw new ServiceException("Le message est vide.");
        }
        Message m = new Message(MockStore.newId(), conversationId, senderId, body == null ? "" : body.trim(),
                LocalDateTime.now());
        m.setAttachmentName(attachmentName);
        store.messages.add(m);
        return m;
    }

    @Override
    public synchronized Conversation openWithArtisan(String clientId, String artisanId) {
        ConversationRecord record = store.conversations.stream()
                .filter(c -> c.clientId().equals(clientId) && c.artisanId().equals(artisanId))
                .findFirst()
                .orElseGet(() -> {
                    ConversationRecord created = new ConversationRecord(MockStore.newId(), null, clientId, artisanId);
                    store.conversations.add(created);
                    return created;
                });
        return toView(record, clientId);
    }

    private Conversation toView(ConversationRecord c, String viewerId) {
        Conversation v = new Conversation();
        v.setId(c.id());
        v.setRequestId(c.requestId());
        if (viewerId.equals(c.clientId())) {
            Artisan a = store.artisan(c.artisanId());
            v.setOtherPartyId(c.artisanId());
            v.setArtisanId(c.artisanId());
            v.setOtherPartyName(a == null ? "Artisan" : a.getDisplayName());
            v.setOtherPartySubtitle(a == null ? "" : a.getTrade().getNameFr());
        } else {
            User client = store.users.get(c.clientId());
            v.setOtherPartyId(c.clientId());
            v.setOtherPartyName(client == null ? "Client" : client.getFullName());
            v.setOtherPartySubtitle("Client");
        }
        List<Message> msgs = store.messages.stream()
                .filter(m -> m.getConversationId().equals(c.id()))
                .sorted(Comparator.comparing(Message::getSentAt))
                .toList();
        if (!msgs.isEmpty()) {
            Message last = msgs.get(msgs.size() - 1);
            v.setLastMessageAt(last.getSentAt());
            v.setLastMessagePreview(last.getAttachmentName() != null && last.getBody().isBlank()
                    ? "Pièce jointe : " + last.getAttachmentName() : last.getBody());
        }
        v.setUnreadCount((int) msgs.stream()
                .filter(m -> !m.getSenderId().equals(viewerId) && m.getReadAt() == null).count());
        return v;
    }
}
