package swiftly;

public class NewMessagesUpdate extends ChatUpdate {
    private final int newMessagesIndex;
    public NewMessagesUpdate(Chat sender, int newMessagesIndex) {
        super(sender, Event.NEW_MESSAGE);
        this.newMessagesIndex = newMessagesIndex;
    }
    public int getNewMessagesIndex() {
        return newMessagesIndex;
    }
}
