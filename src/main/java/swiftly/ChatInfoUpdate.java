package swiftly;

public class ChatInfoUpdate extends ChatUpdate {
    private final ChatInfo newChatInfo;
    private final ChatInfo oldChatInfo;
    public ChatInfoUpdate(Chat sender, ChatInfo oldChatInfo, ChatInfo newChatInfo) {
        super(sender, Event.INFO_UPDATE);
        this.newChatInfo = newChatInfo;
        this.oldChatInfo = oldChatInfo;
    }

    public ChatInfo getOldChatInfo() {
        return oldChatInfo;
    }
    public ChatInfo getNewChatInfo() {
        return newChatInfo;
    }
}
