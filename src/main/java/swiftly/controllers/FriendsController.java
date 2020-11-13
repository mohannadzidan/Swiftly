package swiftly.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class FriendsController {
    public Button allButton;
    public Button onlineButton;
    public Button pendingButton;
    public VBox addFriendPanel;
    public TextField addFriendEmailSearch;
    public VBox allFriendsPanel;

    private Node visiblePanel;
    @FXML
    public void initialize(){
        addFriendPanel.managedProperty().bind(addFriendPanel.visibleProperty());
        allFriendsPanel.managedProperty().bind(allFriendsPanel.visibleProperty());
        addFriendPanel.setVisible(false);
        showPanel(allFriendsPanel);
    }
    public void showAddFriendPanel(ActionEvent actionEvent) {
        showPanel(addFriendPanel);
    }

    public void showAllFriendsPanel(ActionEvent actionEvent) {
        showPanel(allFriendsPanel);
    }

    public void showPanel(Node panel){
        if(visiblePanel == panel)return;
        if(visiblePanel != null) visiblePanel.setVisible(false);
        panel.setVisible(true);
        visiblePanel = panel;
    }
}
