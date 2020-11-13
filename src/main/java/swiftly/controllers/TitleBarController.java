package swiftly.controllers;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import swiftly.SwiftlyApp;

public class TitleBarController {
    public AnchorPane titleBar;
    private double xOffset, yOffset;
    public void minimize(MouseEvent mouseEvent) {
        SwiftlyApp.getInstance().primaryStage.setIconified(true);
    }

    public void maximize(MouseEvent mouseEvent) {
        var app = SwiftlyApp.getInstance();
        app.primaryStage.setMaximized(!app.primaryStage.isMaximized());
    }

    public void exit(MouseEvent mouseEvent) {
        SwiftlyApp.getInstance().primaryStage.close();
    }

    public void windowDrag(MouseEvent mouseEvent) {
        var app = SwiftlyApp.getInstance();
        app.primaryStage.setX(mouseEvent.getScreenX() + xOffset);
        app.primaryStage.setY(mouseEvent.getScreenY() + yOffset);
    }

    public void windowDragStart(MouseEvent mouseEvent) {
        var app = SwiftlyApp.getInstance();
        xOffset = app.primaryStage.getX() - mouseEvent.getScreenX();
        yOffset = app.primaryStage.getY() - mouseEvent.getScreenY();
    }
}
