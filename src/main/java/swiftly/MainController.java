package swiftly;

import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

public class MainController {
    public HBox titleBar;
    public Parent root;
    double xOffset, yOffset;

    public void minimize(MouseEvent mouseEvent) {
        SwiftlyApp.getInstance().primaryStage.setIconified(true);
    }

    public void maximize(MouseEvent mouseEvent) {
        //Main.primaryStage.setMaximized(!Main.primaryStage.isMaximized());
    }

    public void exit(MouseEvent mouseEvent) {
        SwiftlyApp.getInstance().primaryStage.close();
    }

    public void windowDrag(MouseEvent mouseEvent) {
        SwiftlyApp.getInstance().primaryStage.setX(mouseEvent.getScreenX() + xOffset);
        SwiftlyApp.getInstance().primaryStage.setY(mouseEvent.getScreenY() + yOffset);
    }

    public void windowDragStart(MouseEvent mouseEvent) {
        xOffset = SwiftlyApp.getInstance().primaryStage.getX() - mouseEvent.getScreenX();
        yOffset = SwiftlyApp.getInstance().primaryStage.getY() - mouseEvent.getScreenY();
    }
}
