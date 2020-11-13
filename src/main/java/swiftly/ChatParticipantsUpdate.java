package swiftly;

public class ChatParticipantsUpdate extends ChatUpdate {
    private final String[] oldParticipants, newParticipants;

    protected ChatParticipantsUpdate(Chat sender, String[] oldParticipants, String[] newParticipants) {
        super(sender, Event.PARTICIPANTS_UPDATE);
        this.oldParticipants = oldParticipants;
        this.newParticipants = newParticipants;
    }

    public String[] getOldParticipants() {
        return oldParticipants;
    }

    public String[] getNewParticipants() {
        return newParticipants;
    }
}
