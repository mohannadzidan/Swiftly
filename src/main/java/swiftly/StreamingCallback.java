package swiftly;

import java.util.Map;

public interface StreamingCallback {
    void onReceive(Object response);
    void onError(RealtimeDatabase.ErrorCodes code);

}
