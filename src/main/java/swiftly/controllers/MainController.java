package swiftly.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.apache.log4j.Logger;
import swiftly.Chat;
import swiftly.ChatIndex;
import swiftly.SwiftlyApp;
import swiftly.promise.PromiseError;
import swiftly.utils.SwiftlyUtils;
import swiftly.utils.UserUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainController {
    private static final Logger LOGGER = Logger.getRootLogger();
    public HBox root;
    public VBox chatListContainer;
    public HBox friendsButton;
    public AnchorPane statusPanel;
    public ImageView displayIcon;
    public Label displayName;
    public Label status;
    private Parent chatPanel;
    private Parent friendsPanel;
    private ChatController chatController;
    private Parent selectedPanel, selectedNode;
    private HashMap<String, Chat> chats = new HashMap<>();
    //private FriendsController friendsController;

    @FXML
    public void initialize() {
        LOGGER.info("Init main");
        var displayIconClip = new Circle();
        displayIconClip.setCenterX(20);
        displayIconClip.setCenterY(20);
        displayIconClip.setRadius(20);
        displayIcon.setClip(displayIconClip);
        var app = SwiftlyApp.getInstance();
        displayName.setText((String) app.getUser().get("displayName"));
        FXMLLoader chatNodeLoader = new FXMLLoader();
        FXMLLoader friendsNodeLoader = new FXMLLoader();
        try {
            chatPanel = chatNodeLoader.load(SwiftlyApp.class.getResource("/layout/chat.fxml").openStream());
            friendsPanel = friendsNodeLoader.load(SwiftlyApp.class.getResource("/layout/friends.fxml").openStream());
            chatController = chatNodeLoader.getController();
            showFriendsPanel(null);
        } catch (IOException e) {
            LOGGER.error("Couldn't load fxml");
            e.printStackTrace();
        }
        app.database.onValue("users/" + app.getUser().get("localId") + "/chatsIndex")
                .resolved(response -> {
                    /*
                 "chatId":{
                     "lastRead":  1604744669116,
                     "lastUpdate":  160474466913
                 }
                */
                    if (response == null) return;
                    @SuppressWarnings("unchecked")
                    var chatIndexes = (Map<String, Map<String, Object>>) response;
                    var chatIds = chatIndexes.keySet();
                    for (var id : chatIds) {
                        var index = chatIndexes.get(id);
                        var chat = chats.get(id);
                        if (chat == null) {
                            chat = createChat(id);
                            chats.put(id, chat);
                        }
                        chat.update(new ChatIndex(index)).dispatch();

                    }
                }).error(e -> {
                    if(e.getType()== PromiseError.Type.THROWABLE){
                        e.getThrowable().printStackTrace();
                    }
                    LOGGER.error("Failed to read inbox! " + e.getError());
                }).dispatch();


    }

    private Chat createChat(String id) {
        FXMLLoader loader = new FXMLLoader();
        var chat = new Chat(id);
        try {
            var chatListElement = (Parent) loader.load(SwiftlyApp.class.getResource("/layout/chat-list-element.fxml").openStream());
            var controller = (ChatListElementController) loader.getController();
            controller.bind(chat, chatController, this);
            chatListContainer.getChildren().add(chatListElement);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("Failed to add chat element!");
        }
        return chat;
    }

    public void signOut(ActionEvent actionEvent) {
        UserUtils.signOut();
    }

    private void selectPanel(Parent node) {
        if (selectedPanel != node) {

            if (root.getChildren().size() > 1)
                root.getChildren().set(1, node);
            else root.getChildren().add(node);
            selectedPanel = node;
            HBox.setHgrow(node, Priority.ALWAYS);
        }
    }

    public void selectNode(Parent node) {
        if (selectedNode != node) {
            if (selectedNode != null) selectedNode.getStyleClass().remove("selected");
            node.getStyleClass().add("selected");
            selectedNode = node;
        }

    }

    public void showChatPanel() {
        selectPanel(chatPanel);
        displayIcon.setImage(SwiftlyUtils.getImageFromUrl("https://i.pinimg.com/originals/11/38/f8/1138f8398149ac5dd23227d1c4b6f9eb.jpg"));
    }

    public void showFriendsPanel(MouseEvent mouseEvent) {
        selectNode(friendsButton);
        selectPanel(friendsPanel);
    }

    public void showStatusPanel(MouseEvent mouseEvent) {
        statusPanel.setVisible(true);
    }

    public void hideStatusPanel(MouseEvent mouseEvent) {
        statusPanel.setVisible(false);
    }
}
