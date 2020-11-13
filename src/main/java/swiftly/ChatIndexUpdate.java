package swiftly;

public class ChatIndexUpdate extends ChatUpdate {

    private final ChatIndex oldChatIndex, newChatIndex;

    public ChatIndexUpdate(Chat sender, ChatIndex oldChatIndex, ChatIndex newChatIndex) {
        super(sender, Event.LAST_CHECK);
        this.oldChatIndex = oldChatIndex;
        this.newChatIndex = newChatIndex;
    }

    public ChatIndex getOldChatIndex() {
        return oldChatIndex;
    }

    public ChatIndex getNewChatIndex() {
        return newChatIndex;
    }
}
