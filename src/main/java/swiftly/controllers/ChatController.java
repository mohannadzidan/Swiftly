package swiftly.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.apache.log4j.Logger;
import swiftly.Chat;
import swiftly.ChatUpdate;
import swiftly.NewMessagesUpdate;
import swiftly.SwiftlyApp;
import swiftly.broadcast.BroadcastListener;
import swiftly.controllers.messages.Message;
import swiftly.controllers.messages.ReceivedMessage;
import swiftly.controllers.messages.SentMessage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatController {
    private static final Logger LOGGER = Logger.getRootLogger();
    public ImageView contactProfilePicture;
    public Text contactName;
    public Text contactLastSeen;
    public TextField messageInput;
    public VBox messagesContainer;
    public ScrollPane scrollPane;
    public ImageView contactDisplayIcon;
    public Label contactDisplayName;
    private Chat loadedChat;
    private final Stack<SentMessage> sentMessagesControllers = new Stack<>();
    private final Stack<ReceivedMessage> receivedMessagesControllers = new Stack<>();
    private final Stack<Message> inUseControllers = new Stack<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");


    private final BroadcastListener<ChatUpdate> chatUpdateListener = (data) -> {
        if (data.getEvent() == ChatUpdate.Event.NEW_MESSAGE) {
            var newMessagesUpdate = (NewMessagesUpdate) data;
            var messages = data.getSender().getMessages();
            var userId = SwiftlyApp.getInstance().getUser().get("localId");
            LOGGER.debug("Loading chat messages.. " + messages.size());
            reloadMessages(messages, newMessagesUpdate.getNewMessagesIndex());
        }
    };

    public boolean loadChat(Chat chat) {
        if (this.loadedChat == chat) return false;
        if (this.loadedChat != null) loadedChat.removeBroadcastListener(chatUpdateListener);
        chat.addBroadcastListener(chatUpdateListener);
        this.loadedChat = chat;
        releaseAllMessageControllers();
        reloadMessages(chat.getMessages(), 0); // remove all message and load new
        scrollPane.setVvalue(1);
        return true;
    }

    private void releaseAllMessageControllers() {
        var messagesNodes = messagesContainer.getChildren();
        messagesNodes.clear();
        while (!inUseControllers.empty()) releaseMessageController(inUseControllers.pop());
    }

    private void reloadMessages(ArrayList<swiftly.Message> messages, int startIndex) {
        LOGGER.info("Loading messages...");
        var messagesNodes = messagesContainer.getChildren();
        var userId = SwiftlyApp.getInstance().getUser().get("localId");
        for (int i = startIndex; i < messages.size(); i++) {
            var message = messages.get(i);
            Message controller;
            try {
                var msgType = message.getSenderId().equals(userId) ? SentMessage.class : ReceivedMessage.class;
                controller = getMessageController(msgType);
                controller.content.setText(message.getContent());
                controller.time.setText(formatTime(message.getTimestamp()));
                if(msgType == SentMessage.class){
                    ((SentMessage) controller).status.setText("delivered");
                }
                messagesNodes.add(controller.root);
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error("error while loading chat message FXML!");
            }
        }

    }

    private void releaseMessageController(Message controller) {
        if (controller.getClass() == SentMessage.class) {
            sentMessagesControllers.push((SentMessage) controller);
        } else if (controller.getClass() == ReceivedMessage.class) {
            receivedMessagesControllers.push((ReceivedMessage) controller);
        }
    }

    private Message getMessageController(Class<? extends Message> type) throws IOException {
        var loader = new FXMLLoader();
        Message controller;
        if (type == SentMessage.class) {
            if (sentMessagesControllers.empty()) {
                loader.load(SwiftlyApp.class.getResource("/layout/sent-message.fxml").openStream());
                controller = loader.getController();
            } else
                controller = sentMessagesControllers.pop();
        } else {
            if (receivedMessagesControllers.empty()) {
                loader.load(SwiftlyApp.class.getResource("/layout/received-message.fxml").openStream());
                controller = loader.getController();
            } else
                controller = receivedMessagesControllers.pop();
        }
        inUseControllers.push(controller);
        return controller;
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            String s = messageInput.getText().trim();
            messageInput.setText("");
            if (!s.isEmpty()) {

                SentMessage controller;
                try {

                    controller = (SentMessage) getMessageController(SentMessage.class);
                    controller.status.setText("sent");
                    controller.content.setText(s);
                    messagesContainer.getChildren().add(controller.root);
                    scrollPane.setVvalue(1);
                } catch (IOException e) {
                    e.printStackTrace();
                    LOGGER.error("Failed to send the message!");
                    return;
                }
                loadedChat.sendMessage(s).resolved(response -> {
                    if (response.getSuccess()) {
                        controller.status.setText("delivered");
                        LOGGER.debug(response);
                        scrollPane.setVvalue(1);

                    } else {
                        LOGGER.error(response.getRawBody());
                        LOGGER.error("Failed to send the message!");
                        controller.status.setText("failed");
                    }
                }).error(e -> {
                    e.getThrowable().printStackTrace();
                    LOGGER.error("Failed to send the message!");
                    controller.status.setText("failed");
                }).dispatch();

            }

        }
    }
    private String formatTime(long timestamp){
        return dateFormat.format(new Date(timestamp));
    }
}
