package swiftly.utils;

import net.thegreshams.firebase4j.error.FirebaseException;
import net.thegreshams.firebase4j.error.JacksonUtilityException;
import net.thegreshams.firebase4j.model.FirebaseResponse;
import net.thegreshams.firebase4j.service.Firebase;
import org.apache.log4j.Logger;
import swiftly.SwiftlyApp;
import swiftly.UserPublicInfo;
import swiftly.promise.Promise;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public final class UserUtils {

    public static final int
            OK = 0,
            WRONG_EMAIL_OR_PASSWORD = 1,
            USER_DISABLED = 2,
            EMAIL_EXISTS = 3,
            OPERATION_NOT_ALLOWED = 4,
            TOO_MANY_ATTEMPTS_TRY_LATER = 5,
            UNKNOWN = -1;

    private static Firebase auth;
    private static Firebase tokenization;
    private static final Logger LOGGER = Logger.getRootLogger();

    private static final HashMap<String, UserPublicInfo> usersCache = new HashMap<>();

    static {
        try {
            auth = new Firebase("https://identitytoolkit.googleapis.com/v1", false);
            tokenization = new Firebase("https://securetoken.googleapis.com/v1", false);
        } catch (FirebaseException e) {
            auth = null;
            tokenization = null;
            e.printStackTrace();
            LOGGER.error("Couldn't connect to auth/tokenization firebase end-points.");
        }
    }

    public static String refreshIdToken(String refreshToken) {
        if (refreshToken == null) return null;
        var pref = Preferences.userNodeForPackage(SwiftlyApp.class);
        LOGGER.info("Refreshing token");
        try {
            tokenization.addQuery("key", SwiftlyApp.FIREBASE_API_KEY);
            var response = tokenization.post("token", Map.of(
                    "grant_type", "refresh_token",
                    "refresh_token", refreshToken));
            var token = (String) response.getBody().get("id_token");
            pref.put("authRefreshToken", (String) response.getBody().get("refresh_token"));
            // send GET for user data
            updateAppUserData(token);
            return token;
        } catch (Exception | FirebaseException | JacksonUtilityException e) {
            if (SwiftlyApp.getInstance() != null) SwiftlyApp.getInstance().setUser(null);
            pref.remove("authRefreshToken");
        }
        return null;
    }

    public static boolean updateAppUserData(String idToken) {
        LOGGER.info("Fetching user data");
        try {
            auth.addQuery("key", SwiftlyApp.FIREBASE_API_KEY);
            var response = auth.post("accounts:lookup", Map.of("idToken", idToken));
            if (!response.getSuccess()) return false;
            var users = (ArrayList<Object>) response.getBody().get("users");
            if (users.size() > 0) {
                var user = (Map<String, Object>) users.get(0);
                user.put("idToken", idToken);
                SwiftlyApp.getInstance().setUser(user);
                return true;
            }
        } catch (JacksonUtilityException | FirebaseException | UnsupportedEncodingException e) {
            LOGGER.error("couldn't get response!");
            e.printStackTrace();
        }
        return false;
    }

    public static int signIn(String email, String password) {

        Map<String, Object> payload = Map.of(
                "password", password,
                "email", email,
                "returnSecureToken", true
        );
        auth.addQuery("key", SwiftlyApp.FIREBASE_API_KEY);
        FirebaseResponse response = null;
        try {
            response = auth.post("accounts:signInWithPassword", payload);
        } catch (JacksonUtilityException | FirebaseException | UnsupportedEncodingException e) {
            e.printStackTrace();
            LOGGER.error("Error while sign in");
            return UNKNOWN;
        }
        var responseData = response.getBody();
        if (response.getBody().get("error") != null) {
            var errMsg = responseData.get("error").toString();
            if (errMsg.equals("USER_DISABLED")) {
                return USER_DISABLED;
            }
            if (errMsg.equals("EMAIL_NOT_FOUND") || errMsg.equals("INVALID_PASSWORD")) {
                return WRONG_EMAIL_OR_PASSWORD;
            } else {
                return UNKNOWN;
            }
        }
        // save token
        Preferences pref;
        var token = (String) responseData.get("idToken");
        pref = Preferences.userNodeForPackage(SwiftlyApp.class);
        pref.put("authRefreshToken", (String) responseData.get("refreshToken"));
        updateAppUserData(token);
        return OK;
    }

    public static int createAccount(String email, String password, String name) {

        Map<String, Object> payload = Map.of(
                "password", password,
                "email", email,
                "returnSecureToken", true
        );
        auth.addQuery("key", SwiftlyApp.FIREBASE_API_KEY);
        FirebaseResponse response = null;
        try {
            response = auth.post("accounts:signUp", payload);
        } catch (JacksonUtilityException | FirebaseException | UnsupportedEncodingException e) {
            e.printStackTrace();
            LOGGER.error("Error while creating an account");
            return UNKNOWN;
        }
        var responseData = response.getBody();
        if (response.getBody().get("error") != null) {
            LOGGER.error(response.getRawBody());
            LOGGER.error("error while creating account");
            var errMsg = responseData.get("error").toString();
            if (errMsg.equals("EMAIL_EXISTS")) {
                return EMAIL_EXISTS;
            }
            if (errMsg.equals("OPERATION_NOT_ALLOWED")) {
                return WRONG_EMAIL_OR_PASSWORD;
            } else {
                return TOO_MANY_ATTEMPTS_TRY_LATER;
            }
        }
        // save token
        Preferences pref;
        var token = (String) responseData.get("idToken");
        pref = Preferences.userNodeForPackage(SwiftlyApp.class);
        pref.put("authRefreshToken", (String) responseData.get("refreshToken"));
        updateUserDisplayName(token, name);
        updateAppUserData(token);
        return OK;
    }

    public static void updateUserDisplayName(String idToken, String displayName) {
        Map<String, Object> payload = Map.of(
                "idToken", idToken,
                "displayName", displayName,
                "returnSecureToken", true
        );
        auth.addQuery("key", SwiftlyApp.FIREBASE_API_KEY);
        try {
            FirebaseResponse response = auth.post("accounts:update", payload);
            if (!response.getSuccess()) {
                LOGGER.error(response.getRawBody());
                LOGGER.error("Error while updating display name");
            }
        } catch (JacksonUtilityException | FirebaseException | UnsupportedEncodingException e) {
            e.printStackTrace();
            LOGGER.error("Error while updating display name");
        }
    }

    @SuppressWarnings("unchecked")
    public static Promise<UserPublicInfo> getUserInfo(String id) {
        if (usersCache.get(id) != null && !usersCache.get(id).isOutOfDate()) {
            return new Promise<>((resolve, exception) -> {
                resolve.run(usersCache.get(id));
            });
        }
        var app = SwiftlyApp.getInstance();
        return (Promise<UserPublicInfo>) app.database.getAsync("/users/" + id + "/public").resolved(result -> {
            usersCache.put(id, new UserPublicInfo(id, result.getBody()));
        }).then(new Promise<UserPublicInfo>((resolve, exceptionHandler) ->
                resolve.run(usersCache.get(id))
        ));
    }

    public static void signOut() {
        var pref = Preferences.userNodeForPackage(SwiftlyApp.class);
        pref.remove("authRefreshToken");
    }
}
