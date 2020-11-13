package swiftly.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import org.apache.log4j.Logger;
import swiftly.SwiftlyApp;
import swiftly.utils.UserUtils;

import java.io.IOException;
import java.util.prefs.Preferences;


public class SignInController {
    private static final Logger LOGGER = Logger.getRootLogger();
    private static final String PASSWORDS_MISMATCH_CONFIRMATION = "The password doesn't match the confirmation!";
    private static final String WRONG_PASSWORD_MESSAGE = "Wrong password!";
    private static final String UNKNOWN_ERROR = "Unknown error occurred!";
    private static final String USER_DISABLED_MESSAGE = "Your account is temporary suspended!";
    private static final String EMAIL_EXISTS = "The email already registered!";
    private static final String OPERATION_NOT_ALLOWED = "This operation cannot be done now, please try again later";
    private static final String TOO_MANY_ATTEMPTS_TRY_LATER = "Too many attempts, please try again later";
    private static final String EMPTY_FIELD = "Please fill all the required fields";

    public PasswordField passwordField;
    public TextField emailField;
    public Label signInMessage;
    public Button signInButton;
    public TextField registerName;
    public TextField registerEmail;
    public PasswordField registerPassword;
    public PasswordField registerPasswordConfirm;
    public Button createAccountButton;
    public Label createAccountMessage;

    @FXML
    public void initialize() {
        // check if we have valid token
        var pref = Preferences.userNodeForPackage(SwiftlyApp.class);
        // try log in
        String refreshToken = pref.get("authRefreshToken", null);
        String token;
        if ((token = UserUtils.refreshIdToken(refreshToken)) != null && UserUtils.updateAppUserData(token)) {
            try {
                var mainScreen = (Parent) FXMLLoader.load(SwiftlyApp.class.getResource("/layout/main.fxml"));
                SwiftlyApp.getInstance().selectContent(mainScreen);
            } catch (IOException e) {
                LOGGER.error("Loading main screen failed!");
                e.printStackTrace();
            }
        }
        emailField.setText(pref.get("userEmail", ""));
    }

    public void onForgotPassword(MouseEvent mouseEvent) {
        System.out.println("Reset password!");
    }

    public void signIn(ActionEvent actionEvent) {

        int signInStatus = UserUtils.signIn(emailField.getText(), passwordField.getText());
        switch (signInStatus) {
            case UserUtils.OK:
                try {
                    var mainScreen = (Parent) FXMLLoader.load(SwiftlyApp.class.getResource("/layout/main.fxml"));
                    SwiftlyApp.getInstance().selectContent(mainScreen);
                } catch (IOException e) {
                    LOGGER.error("Loading main screen failed!");
                    e.printStackTrace();
                }

                break;
            case UserUtils.WRONG_EMAIL_OR_PASSWORD:
                signInMessage.setText(WRONG_PASSWORD_MESSAGE);
                break;
            default:
                signInMessage.setText(UNKNOWN_ERROR);
        }
    }


    public void createAccount(ActionEvent actionEvent) {
        if (registerPassword.getText().isEmpty() ||
                registerEmail.getText().isEmpty() ||
                registerName.getText().isEmpty() ||
                registerPasswordConfirm.getText().isEmpty()
        ) {
            createAccountMessage.setText(EMPTY_FIELD);
            return;
        }
        if (!registerPassword.getText().equals(registerPasswordConfirm.getText())) {
            createAccountMessage.setText(PASSWORDS_MISMATCH_CONFIRMATION);
            return;
        }
        int status = UserUtils.createAccount(registerEmail.getText(), registerPassword.getText(), registerName.getText());
        switch (status) {
            case UserUtils.OK:
                try {
                    var mainScreen = (Parent) FXMLLoader.load(SwiftlyApp.class.getResource("/layout/main.fxml"));
                    SwiftlyApp.getInstance().selectContent(mainScreen);
                } catch (IOException e) {
                    LOGGER.error("Loading main screen failed!");
                    e.printStackTrace();
                }
                break;
            case UserUtils.EMAIL_EXISTS:
                createAccountMessage.setText(EMAIL_EXISTS);
                break;
            case UserUtils.OPERATION_NOT_ALLOWED:
                createAccountMessage.setText(OPERATION_NOT_ALLOWED);
                break;
            case UserUtils.TOO_MANY_ATTEMPTS_TRY_LATER:
                createAccountMessage.setText(TOO_MANY_ATTEMPTS_TRY_LATER);
                break;
            default:
                createAccountMessage.setText(UNKNOWN_ERROR);
        }
    }
}
