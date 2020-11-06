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
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class SwiftlyApp extends Application {
    private static final Logger LOGGER = Logger.getRootLogger();
    private static SwiftlyApp instance;
    public static final String FIREBASE_API_KEY = "AIzaSyBdmp-D0EiHlJRHz2haf65NgZrX0XciIIg";
    public Stage primaryStage;
    private ObservableList<Node> dynamicContent;
    private Parent selectedDynamicElement;
    private Map<String, Parent> dynamicElements;
    private String lateDynamicContent;

    public static SwiftlyApp getInstance() {
        if (instance == null) throw new RuntimeException("application didn't start yet!");
        return instance;
    }

    public static void main(String[] args) throws IOException {
        var database = new RealtimeDatabase("https://swiftly-chat-app.firebaseio.com");
        var handle = database.onValue( new StreamingCallback() {

            @Override
            public void onReceive(Object response) {

            }

            @Override
            public void onError(RealtimeDatabase.ErrorCodes code) {
                LOGGER.debug("received:"+code);
            }


        }, 2000, "verifyPassword");

        /////////////////////////////////
        //launch(args);
    }




    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        SwiftlyApp.instance.primaryStage = primaryStage;
        // load dynamic content
        dynamicElements = Map.of(
                "login", FXMLLoader.load(getClass().getResource("/layout/login.fxml")),
                "main", FXMLLoader.load(getClass().getResource("/layout/main.fxml"))
        );
        // load root
        Parent root = FXMLLoader.load(getClass().getResource("/layout/root.fxml"));
        dynamicContent = ((Pane) root).getChildren();
        primaryStage.setScene(new Scene(root));
        primaryStage.initStyle(StageStyle.UNDECORATED);
        if (lateDynamicContent != null) {
            selectContent(lateDynamicContent);
        } else {
            selectContent("login");
        }
        primaryStage.show();
    }

    public void selectContent(String content) {
        if (dynamicElements == null) {
            lateDynamicContent = content;
            return;
        }
        var element = dynamicElements.get(content);
        if (element == null || element == selectedDynamicElement) return;
        dynamicContent.remove(selectedDynamicElement);
        dynamicContent.add(element);
        selectedDynamicElement = element;
        primaryStage.sizeToScene();
    }
}
