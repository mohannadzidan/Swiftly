package swiftly;

import java.util.Map;

public class ChatIndex {
    private final long lastMessageTimestamp;
    private final long lastCheckTimestamp;
    private final long lastInfoUpdateTimestamp;
    private final long lastParticipantsUpdateTimestamp;

    public ChatIndex(long lastUpdateTimestamp, long lastCheckTimestamp, long lastInfoUpdateTimestamp, long lastParticipantsUpdateTimestamp) {
        this.lastMessageTimestamp = lastUpdateTimestamp;
        this.lastCheckTimestamp = lastCheckTimestamp;
        this.lastInfoUpdateTimestamp = lastInfoUpdateTimestamp;
        this.lastParticipantsUpdateTimestamp = lastParticipantsUpdateTimestamp;

    }

    public ChatIndex(Map<String, Object> deserializedJson) {
        this.lastMessageTimestamp = Long.parseLong(deserializedJson.getOrDefault("lastMessageTimestamp", "0").toString());
        this.lastCheckTimestamp = Long.parseLong(deserializedJson.getOrDefault("lastCheckTimestamp", "0").toString());
        this.lastInfoUpdateTimestamp = Long.parseLong(deserializedJson.getOrDefault("lastInfoUpdateTimestamp", "0").toString());
        this.lastParticipantsUpdateTimestamp = Long.parseLong(deserializedJson.getOrDefault("lastParticipantsUpdateTimestamp", "0").toString());
    }


    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public long getLastCheckTimestamp() {
        return lastCheckTimestamp;
    }

    public long getLastInfoUpdateTimestamp() {
        return lastInfoUpdateTimestamp;
    }

    public long getLastParticipantsUpdateTimestamp() {
        return lastParticipantsUpdateTimestamp;
    }
}
