package swiftly;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import net.thegreshams.firebase4j.service.Firebase;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.logging.ErrorManager;
import java.util.prefs.Preferences;


public class SignInController implements Initializable {
    private static final Logger LOGGER = Logger.getRootLogger();
    static String WRONG_PASSWORD_MESSAGE = "Wrong password!";
    static String UNKNOWN_ERROR = "Unknown error occured!";
    static String USER_DISABLED_MESSAGE = "Your account is temporary suspended!";

    public PasswordField passwordField;
    public TextField emailField;
    public Text signInMessage;
    public Button signInButton;
    private Firebase auth;
    private Firebase securetoken;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            auth = new Firebase("https://identitytoolkit.googleapis.com/v1", false);
            securetoken = new Firebase("https://securetoken.googleapis.com/v1", false);
        } catch (FirebaseException e) {
            LOGGER.error("Failed to initialize firebase!");
            LOGGER.error(e);
        }
        var pref = Preferences.userNodeForPackage(SwiftlyApp.class);
        // check if we have valid token
        long authTokenExpiryMillis = Long.parseLong(pref.get("authTokenExpiryMillis", "0"));
        if (authTokenExpiryMillis > System.currentTimeMillis()) {
            SwiftlyApp.getInstance().selectContent("main"); // login
        }else{
        }
        emailField.setText(pref.get("userEmail", ""));


    }

    public void onForgotPassword(MouseEvent mouseEvent) {
        System.out.println("Reset password!");
    }

    public void onLoginButton(ActionEvent actionEvent) throws JacksonUtilityException, UnsupportedEncodingException, FirebaseException {
        signInMessage.setText(null);
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("password", passwordField.getText());
        payload.put("email", emailField.getText());
        payload.put("returnSecureToken", true);
        auth.addQuery("key", SwiftlyApp.FIREBASE_API_KEY);
        var response = auth.post("accounts:signInWithPassword", payload);
        var responseData = response.getBody();

        if (response.getCode() != 200) {
            var error = (LinkedHashMap<String, Object>) responseData.get("error");
            System.out.println(error.get("message"));
            if (error.get("message").equals("USER_DISABLED")) {
                signInMessage.setText(USER_DISABLED_MESSAGE);
            } else {
                signInMessage.setText(WRONG_PASSWORD_MESSAGE);
            }
        } else if (response.getBody().get("error") != null) {
            signInMessage.setText(UNKNOWN_ERROR);
        } else { // success
            // save token
            Preferences pref;
            pref = Preferences.userNodeForPackage(SwiftlyApp.class);
            pref.put("userEmail", emailField.getText());
            pref.put("authRefreshToken", (String) responseData.get("refreshToken"));
            pref.put("authIdToken", (String) responseData.get("idToken"));
            long tokenLifetime = Long.parseLong((String) responseData.get("expiresIn")) - 30; // 30 secs before expire
            long authTokenExpiryMillis = System.currentTimeMillis() + tokenLifetime * 1000;
            pref.put("authTokenExpiryMillis", Long.toString(authTokenExpiryMillis));
            SwiftlyApp.getInstance().selectContent("main");
        }

    }


}
