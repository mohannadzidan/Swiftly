package swiftly;

public class FirebaseError {
    private final int code;
    private final String message;

    public FirebaseError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
