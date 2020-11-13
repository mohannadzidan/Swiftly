package swiftly;

import java.util.Map;

public class UserPublicInfo {
    private final String id, displayName, email, status;
    private final long creationTime;

    public UserPublicInfo(String id, String displayName, String email, String status) {
        this.displayName = displayName;
        this.email = email;
        this.status = status;
        this.id = id;
        this.creationTime = System.currentTimeMillis();
    }
    public UserPublicInfo(String id, Map<String, Object> deserializedJson) {
        this.displayName = (String) deserializedJson.getOrDefault("displayName", "SwiftlyUser");
        this.email = (String) deserializedJson.getOrDefault("email", "");
        this.status = (String) deserializedJson.getOrDefault("status", "offline");;
        this.id = id;
        this.creationTime = System.currentTimeMillis();
    }


    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }
    public boolean isOutOfDate(){
        return System.currentTimeMillis() - creationTime > 60*1000*5; // every 5 minutes
    }

    public String getId() {
        return id;
    }
}
