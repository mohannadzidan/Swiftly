package swiftly;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.thegreshams.firebase4j.error.FirebaseException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class SwiftlyApp extends Application {
    static {
        System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");
    }
    private static final Logger LOGGER = Logger.getRootLogger();
    private static SwiftlyApp instance;
    public static final String FIREBASE_API_KEY = "AIzaSyBdmp-D0EiHlJRHz2haf65NgZrX0XciIIg";
    public static final String FIREBASE_REALTIME_DATABASE = "https://swiftly-chat-app.firebaseio.com";
    public Stage primaryStage;
    public RealtimeDatabase database;
    private ObservableList<Node> dynamicContent;
    private Map<String, Object> user;

    public static SwiftlyApp getInstance() {
        if (instance == null) throw new RuntimeException("application didn't start yet!");
        return instance;
    }

    public static void main(String[] args) throws IOException {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        this.primaryStage = primaryStage;
        try {
            database = new RealtimeDatabase(FIREBASE_REALTIME_DATABASE, 5, Platform::runLater);
        } catch (FirebaseException e) {
            e.printStackTrace();
            LOGGER.error("couldn't start firebase!, see exception above");
            return;
        }
        Pane root = FXMLLoader.load(getClass().getResource("/layout/root.fxml"));
        dynamicContent = root.getChildren();
        primaryStage.setScene(new Scene(root));
        primaryStage.initStyle(StageStyle.UNDECORATED);
        var signIn = (Node) FXMLLoader.load(getClass().getResource("/layout/login.fxml"));
        if (user == null) {
            // not signed in yet
            selectContent(signIn);
        }
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        database.shutdown(); // close all connections
        super.stop();
        System.out.println("close");
    }

    public void selectContent(Node node) {
        if (node == null) return;
        VBox.setVgrow(node, Priority.ALWAYS);
        if (dynamicContent.size() > 1) {
            dynamicContent.set(1, node);
        } else {
            dynamicContent.add(node);
        }
        primaryStage.sizeToScene();
    }

    public Map<String, Object> getUser() {
        return user;
    }

    public void setUser(Map<String, Object> user) {
        this.user = user;
    }
}
