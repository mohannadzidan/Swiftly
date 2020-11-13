package swiftly;

import java.util.Map;

public class ChatInfo {
    public enum Type{DIRECT, GROUP}
    private final String name;
    private final Type type;

    public ChatInfo(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public ChatInfo(Map<String, Object> deserializedJson) {
        this.name = (String) deserializedJson.getOrDefault("name", null);
        var type = deserializedJson.getOrDefault("type", "direct");
        this.type = type.equals("direct")? Type.DIRECT : Type.GROUP;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }
}
