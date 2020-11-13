package swiftly.controllers;

import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import net.thegreshams.firebase4j.error.FirebaseException;
import org.apache.log4j.Logger;
import swiftly.Chat;
import swiftly.SwiftlyApp;
import swiftly.UserPublicInfo;
import swiftly.promise.Promise;
import swiftly.promise.PromiseError;
import swiftly.utils.UserUtils;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatListElementController {
    private static final Logger LOGGER = Logger.getRootLogger();
    public Label lastMessage;
    public Label name;
    public ImageView icon;
    public HBox root;
    public StackPane notificationIndicator;
    private ActionListener clickCallback; // TODO create custom listener interface instead

    public void setNotified(boolean state) {
        var styleClass= root.getStyleClass();
        if(state){
            if(!styleClass.contains("notified"))styleClass.add("notified");
        }else{
            styleClass.remove("notified");
        }
    }

    public void onClick(MouseEvent mouseEvent) {
        if(clickCallback != null) clickCallback.actionPerformed(null);
    }

    public void setClickCallback(ActionListener clickCallback) {
        this.clickCallback = clickCallback;
    }

    public void bind(Chat chat, ChatController chatController, MainController mainController){
        chat.addBroadcastListener((data) -> {
            switch (data.getEvent()) {
                case NEW_MESSAGE:
                    var sender = data.getSender();
                    if (sender.getMessages().size() > 0) {
                        var lastMessage = sender.getMessages().get(sender.getMessages().size() - 1);
                        this.lastMessage.setText(lastMessage.getContent());
                    }
                    break;
                case LAST_CHECK:
                    setNotified(false);
                    break;
                case INFO_UPDATE:
                    var chatName = chat.getChatInfo().getName();
                    if(chatName == null){
                        var participantsIds = chat.getParticipants();
                        var participants = new ArrayList<UserPublicInfo>(participantsIds.length);
                        var promises = new Promise<?>[participantsIds.length];
                        for(int i = 0; i< participantsIds.length; i++){
                            promises[i] = UserUtils.getUserInfo(participantsIds[i]).resolved(participants::add);
                        }
                        Promise.all(Arrays.asList(promises)).resolved(res -> {
                           name.setText( participantsInfoToChatName(participants));
                        }).error( e -> {
                            LOGGER.error(e);
                            LOGGER.error("Failed to get participants");
                        }).dispatch();

                    }else{
                        name.setText(chat.getChatInfo().getName());
                    }
                    break;
            }

        });
        setClickCallback((ignored) -> {
            mainController.selectNode(root);
            chatController.loadChat(chat);
            mainController.showChatPanel();
            chat.updateLastCheck();
        });
    }

    private String participantsInfoToChatName(List<UserPublicInfo> participants){
        StringBuilder builder = new StringBuilder();
        var userId = SwiftlyApp.getInstance().getUser().get("localId");
            builder.append("You").append(", ");
        for (var participant: participants ) {
            if(!participant.getId().equals(userId)){
                builder.append(participant.getDisplayName()).append(", ");
            }
        }
        builder.setLength(builder.length()-2); // remove last ", "
        return builder.toString();
    }
}
