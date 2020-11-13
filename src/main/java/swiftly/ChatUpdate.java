package swiftly;

import swiftly.broadcast.BroadcastData;

public abstract class ChatUpdate extends BroadcastData<Chat> {
    public enum Event {NEW_MESSAGE, MESSAGE_DELIVERED, MESSAGE_READ, LAST_CHECK, INFO_UPDATE, PARTICIPANTS_UPDATE}
    protected final Event event;
    protected ChatUpdate(Chat sender, Event event) {
        super(sender);
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }
}
