package swiftly;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.service.Firebase;
import org.apache.log4j.Logger;

public class SwiftlyApp extends Application {
    public static final Logger LOGGER = Logger.getRootLogger();
    private static SwiftlyApp instance;
    public Scene mainScene;
    public Firebase realtimeDatabase;
    public Firebase auth;
    public String FIREBASE_API_KEY = "AIzaSyBdmp-D0EiHlJRHz2haf65NgZrX0XciIIg";
    public Stage primaryStage;
    private ObservableList<Node> dynamicContent;
    private Parent signInContent;
    private Pane mainContent;

    public static SwiftlyApp getInstance() {
        if (instance == null) throw new RuntimeException("application didn't start yet!");
        return instance;
    }

    public static void main(String[] args) {

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        // initialize firebase
        try {
            realtimeDatabase = new Firebase("https://swiftly-chat-app.firebaseio.com/");
            auth = new Firebase("https://identitytoolkit.googleapis.com/v1", false);
        } catch (FirebaseException e) {
            LOGGER.error("Failed to initialize firebase!");
            LOGGER.error(e);
        }
        SwiftlyApp.instance.primaryStage = primaryStage;
        // load dynamic content
        signInContent = FXMLLoader.load(getClass().getResource("/layout/login.fxml"));
        mainContent = FXMLLoader.load(getClass().getResource("/layout/main.fxml"));
        // load root
        Parent root = FXMLLoader.load(getClass().getResource("/layout/root.fxml"));
        dynamicContent = ((Pane) root.lookup("#root")).getChildren();
        mainScene = new Scene(root);
        primaryStage.setScene(mainScene);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setResizable(false);
        selectContent("signIn");
        primaryStage.show();
    }

    public void selectContent(String content) {
        switch (content) {
            case "signIn":
                dynamicContent.clear();
                dynamicContent.add(signInContent);
                primaryStage.sizeToScene();

                break;
            case "main":
                dynamicContent.clear();
                dynamicContent.add(mainContent);
                primaryStage.sizeToScene();

                break;
            default:
                throw new RuntimeException("Unknown content!");
        }
    }
}
