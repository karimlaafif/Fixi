package ma.fixi.controller;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import ma.fixi.model.Conversation;
import ma.fixi.model.Message;
import ma.fixi.service.Services;
import ma.fixi.util.Async;
import ma.fixi.util.Formats;
import ma.fixi.view.Navigator;
import ma.fixi.view.Page;
import ma.fixi.view.Route;
import ma.fixi.view.Session;
import ma.fixi.view.component.Toast;
import ma.fixi.view.component.Ui;

/** Messagerie client ↔ artisan. Appelle {@code MessageService}. */
public class MessagesController implements Page {

    @FXML private Label countLabel;
    @FXML private TextField filterField;
    @FXML private VBox conversationsBox;
    @FXML private VBox chatPane;
    @FXML private HBox chatHeader;
    @FXML private ScrollPane messagesScroll;
    @FXML private VBox messagesBox;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private VBox emptyPane;

    private List<Conversation> conversations = new ArrayList<>();
    private Conversation current;
    private String userId;

    @FXML
    private void initialize() {
        filterField.textProperty().addListener((o, a, b) -> renderConversations());
        sendButton.disableProperty().bind(messageField.textProperty().isEmpty());
    }

    @Override
    public void onShow(Route route, Object param) {
        userId = Session.user().getId();
        loadConversations(param instanceof String id ? id : null);
    }

    private void loadConversations(String openId) {
        Async.run(() -> Services.messages().findConversations(userId), list -> {
            conversations = new ArrayList<>(list);
            countLabel.setText("Conversations (" + conversations.size() + ")");
            if (conversations.isEmpty()) {
                showEmpty();
                return;
            }
            Conversation toOpen = conversations.stream().filter(c -> c.getId().equals(openId)).findFirst()
                    .orElse(current != null && conversations.contains(current) ? current : conversations.get(0));
            open(toOpen);
        }, e -> Toast.error(conversationsBox, Async.userMessage(e)));
    }

    private void showEmpty() {
        chatPane.setVisible(false);
        emptyPane.setVisible(true);
        emptyPane.getChildren().setAll(Ui.emptyState("fth-message-square", "Aucune conversation",
                "Utilisez le bouton « Contacter » sur le profil d'un artisan pour démarrer une discussion.", null));
        renderConversations();
    }

    private void renderConversations() {
        conversationsBox.getChildren().clear();
        String q = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase(Locale.ROOT);
        for (Conversation c : conversations) {
            if (!q.isEmpty() && !c.getOtherPartyName().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            Label name = Ui.label(c.getOtherPartyName(), "conversation-name");
            Label time = Ui.label(Formats.conversationTime(c.getLastMessageAt()), "text-subtle");
            HBox top = new HBox(8, name, Ui.hSpacer(), time);
            top.setAlignment(Pos.CENTER_LEFT);

            String preview = c.getLastMessagePreview() == null ? "Nouvelle conversation" : c.getLastMessagePreview();
            Label previewLabel = Ui.label(preview, "conversation-preview");
            previewLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(previewLabel, Priority.ALWAYS);
            HBox bottom = new HBox(8, previewLabel);
            bottom.setAlignment(Pos.CENTER_LEFT);
            if (c.getUnreadCount() > 0 && !c.equals(current)) {
                previewLabel.getStyleClass().add("unread");
                bottom.getChildren().add(Ui.label(String.valueOf(c.getUnreadCount()), "badge-count"));
            }
            VBox text = new VBox(3, top, Ui.label(c.getOtherPartySubtitle(), "text-subtle"), bottom);
            HBox.setHgrow(text, Priority.ALWAYS);
            text.setMinWidth(0);

            HBox row = new HBox(12, Ui.avatar(c.getOtherPartyName(), 46, false), text);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("conversation-row");
            if (c.equals(current)) {
                row.getStyleClass().add("selected");
            }
            row.setOnMouseClicked(e -> open(c));
            conversationsBox.getChildren().add(row);
        }
    }

    private void open(Conversation c) {
        current = c;
        c.setUnreadCount(0);
        chatPane.setVisible(true);
        emptyPane.setVisible(false);
        renderConversations();
        renderHeader();
        Async.run(() -> Services.messages().findMessages(c.getId(), userId), this::renderMessages,
                e -> Toast.error(messagesBox, Async.userMessage(e)));
        Platform.runLater(messageField::requestFocus);
    }

    private void renderHeader() {
        VBox text = new VBox(2, Ui.label(current.getOtherPartyName(), "h3"),
                Ui.label(current.getOtherPartySubtitle(), "text-muted"));
        chatHeader.getChildren().setAll(Ui.avatar(current.getOtherPartyName(), 48, false), text, Ui.hSpacer());
        if (current.getArtisanId() != null) {
            Button profile = Ui.button("Voir le profil", "btn-ghost", "fth-user");
            profile.getStyleClass().add("btn-small");
            String artisanId = current.getArtisanId();
            profile.setOnAction(e -> Navigator.goTo(Route.ARTISAN_PROFILE, artisanId));
            chatHeader.getChildren().add(profile);
        }
    }

    private void renderMessages(List<Message> messages) {
        messagesBox.getChildren().clear();
        if (messages.isEmpty()) {
            messagesBox.getChildren().add(Ui.emptyState("fth-message-circle", "Démarrez la conversation",
                    "Présentez votre besoin : l'artisan reçoit une notification.", null));
            return;
        }
        LocalDate lastDay = null;
        for (Message m : messages) {
            LocalDate day = m.getSentAt().toLocalDate();
            if (!day.equals(lastDay)) {
                Label sep = Ui.label(Formats.capitalize(dayLabel(day)), "day-separator");
                HBox center = new HBox(sep);
                center.setAlignment(Pos.CENTER);
                messagesBox.getChildren().add(center);
                lastDay = day;
            }
            messagesBox.getChildren().add(bubble(m));
        }
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    private static String dayLabel(LocalDate day) {
        String relative = Formats.relativeDay(day);
        return relative.contains("/") ? Formats.longDate(day) : relative;
    }

    private HBox bubble(Message m) {
        boolean mine = m.getSenderId().equals(userId);
        VBox content = new VBox(6);
        if (m.getBody() != null && !m.getBody().isBlank()) {
            Label body = new Label(m.getBody());
            body.setWrapText(true);
            content.getChildren().add(body);
        }
        if (m.getAttachmentName() != null) {
            Label file = new Label(m.getAttachmentName(), Ui.icon(
                    m.getAttachmentName().toLowerCase(Locale.ROOT).endsWith(".pdf") ? "fth-file-text" : "fth-paperclip", 18));
            file.setGraphicTextGap(10);
            file.getStyleClass().addAll("attachment", "semibold");
            content.getChildren().add(file);
        }
        String time = Formats.time(m.getSentAt()) + (mine && m.getReadAt() != null ? "  ·  Lu" : "");
        Label timeLabel = Ui.label(time, "bubble-time");
        HBox timeRow = new HBox(timeLabel);
        timeRow.setAlignment(Pos.CENTER_RIGHT);
        content.getChildren().add(timeRow);
        content.getStyleClass().add("bubble");
        if (mine) {
            content.getStyleClass().add("mine");
        }
        content.setMaxWidth(460);

        HBox row = new HBox(content);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    @FXML
    private void onSend() {
        send(messageField.getText(), null);
    }

    @FXML
    private void onAttach() {
        if (current == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Joindre un fichier");
        File f = chooser.showOpenDialog(Navigator.stage());
        if (f != null) {
            send(messageField.getText(), f.getName());
        }
    }

    private void send(String body, String attachment) {
        if (current == null || ((body == null || body.isBlank()) && attachment == null)) {
            return;
        }
        String conversationId = current.getId();
        messageField.clear();
        Async.run(() -> Services.messages().send(conversationId, userId, body, attachment),
                sent -> loadConversations(conversationId),
                e -> {
                    messageField.setText(body);
                    Toast.error(messagesBox, Async.userMessage(e));
                });
    }
}
