package swiftly;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class SignInController {
    static  String WRONG_PASSWORD_MESSAGE = "Wrong password!";
    static  String UNKNOWN_ERROR = "Unknown error occured!";
    static  String USER_DISABLED_MESSAGE = "Your account is temporary suspended!";

    public PasswordField passwordField;
    public TextField emailField;
    public Text signInMessage;
    public Button signInButton;
    public void onForgotPassword(MouseEvent mouseEvent) {
        System.out.println("Reset password!");
    }

    public void onLoginButton(ActionEvent actionEvent) throws JacksonUtilityException, UnsupportedEncodingException, FirebaseException {
        signInMessage.setText(null);
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("password", passwordField.getText());
        payload.put("email", emailField.getText());
        payload.put("returnSecureToken", true);
        SwiftlyApp.getInstance().auth.addQuery("key", SwiftlyApp.getInstance().FIREBASE_API_KEY);
        var response = SwiftlyApp.getInstance().auth.post("accounts:signInWithPassword", payload);
        if(response.getCode() != 200){
            var error = (LinkedHashMap<String, Object>) response.getBody().get("error");
            System.out.println(error.get("message"));
            if(error.get("message").equals("USER_DISABLED")){
                signInMessage.setText(USER_DISABLED_MESSAGE);
            }else{
                signInMessage.setText(WRONG_PASSWORD_MESSAGE);
            }
        }else if(response.getBody().get("error") != null){
            signInMessage.setText(WRONG_PASSWORD_MESSAGE);
        }else{ // success
            SwiftlyApp.getInstance().selectContent("main");
        }

    }
}
