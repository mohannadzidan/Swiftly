package swiftly;

import net.thegreshams.firebase4j.model.FirebaseResponse;
import org.apache.log4j.Logger;
import swiftly.broadcast.Broadcast;
import swiftly.broadcast.BroadcastListener;
import swiftly.promise.Promise;
import swiftly.promise.PromiseError;

import javax.lang.model.type.NullType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Chat implements Broadcast<ChatUpdate> {


    private static final Logger LOGGER = Logger.getRootLogger();
    private final ArrayList<Message> messages = new ArrayList<>();
    private final String chatId;
    private final ArrayList<BroadcastListener<ChatUpdate>> listeners = new ArrayList<>();
    private ChatIndex index;
    private ChatInfo chatInfo;
    private String[] participants;

    public Chat(String chatId) {
        this.chatId = chatId;
    }

    public Promise<NullType> update(ChatIndex newIndex) {
        var oldIndex = index;
        index = newIndex;
        broadcastAll(new ChatIndexUpdate(this, oldIndex, newIndex));
        ArrayList<Promise<?>> promises = new ArrayList<>(2);
        if (oldIndex == null) {
            promises.add(updateParticipants()
                    .resolved(res -> {
                        var oldParticipants = participants;
                        participants = res;
                        broadcastAll(new ChatParticipantsUpdate(this, oldParticipants, res));
                    }).error(LOGGER::error));
            promises.add(updateMessages()
                    .resolved(this::broadcastAll)
                    .error(e -> {
                        if (e.getType() == PromiseError.Type.THROWABLE) {
                            e.getThrowable().printStackTrace();
                        } else {
                            var firebaseError = (FirebaseError) e.getError();
                            LOGGER.error(firebaseError.getCode() + ":" + firebaseError.getMessage());
                        }
                        LOGGER.error("failed to update chat!");
                    }));
            promises.add(updateChatInfo()
                    .resolved(res -> {
                        var oldChatInfo = chatInfo;
                        this.chatInfo = res;
                        broadcastAll(new ChatInfoUpdate(this, oldChatInfo, res));
                    })
                    .error(e -> {
                        if (e.getType() == PromiseError.Type.THROWABLE) {
                            e.getThrowable().printStackTrace();
                        } else {
                            var firebaseError = (FirebaseError) e.getError();
                            LOGGER.error(firebaseError.getCode() + ":" + firebaseError.getMessage());
                        }
                        LOGGER.error("failed to update chat!");
                    }));


        } else {
            if (newIndex.getLastMessageTimestamp() != oldIndex.getLastMessageTimestamp()) {
                promises.add(updateMessages().resolved(this::broadcastAll)
                        .error(e -> {
                            if (e.getType() == PromiseError.Type.THROWABLE) {
                                e.getThrowable().printStackTrace();
                            } else {
                                var firebaseError = (FirebaseError) e.getError();
                                LOGGER.error(firebaseError.getCode() + ":" + firebaseError.getMessage());
                            }
                            LOGGER.error("failed to update chat!");
                        }));
            }
            if (newIndex.getLastInfoUpdateTimestamp() != oldIndex.getLastInfoUpdateTimestamp()) {
                promises.add(updateChatInfo().resolved(res -> {
                    var oldChatInfo = chatInfo;
                    this.chatInfo = res;
                    broadcastAll(new ChatInfoUpdate(this, oldChatInfo, res));
                }).error(e -> {
                    if (e.getType() == PromiseError.Type.THROWABLE) {
                        e.getThrowable().printStackTrace();
                    } else {
                        var firebaseError = (FirebaseError) e.getError();
                        LOGGER.error(firebaseError.getCode() + ":" + firebaseError.getMessage());
                    }
                    LOGGER.error("failed to update chat!");
                }));
            }
        }
        return Promise.all(promises);
    }

    @SuppressWarnings("unchecked")
    private Promise<String[]> updateParticipants() {
        var app = SwiftlyApp.getInstance();
        var result = new AtomicReference<FirebaseResponse>();
        var error = new AtomicReference<PromiseError>();
        return (Promise<String[]>) app.database.getAsync("/chats/" + chatId + "/participants"
                , RealtimeDatabase.query("shallow", "true"))
                .resolved(result::set)
                .error(error::set)
                .then(new Promise<String[]>((resolve, errorHandler) -> {
                    if (error.get() != null) {
                        LOGGER.error("error while updating participants! " + error.get());
                        errorHandler.handle(error.get());
                        return;
                    }
                    var response = result.get();
                    if (!response.getSuccess()) {
                        errorHandler.handle(new PromiseError(response.getBody().get("error")));
                        return;
                    }
                    var body = response.getBody();
                    var participants = new String[body.keySet().size()];
                    body.keySet().toArray(participants);
                    resolve.run(participants);
                }));
    }

    @SuppressWarnings("unchecked")
    private Promise<ChatInfo> updateChatInfo() {
        var app = SwiftlyApp.getInstance();
        var result = new AtomicReference<FirebaseResponse>();
        var error = new AtomicReference<PromiseError>();
        return (Promise<ChatInfo>) app.database.getAsync("/chats/" + chatId + "/info")
                .resolved(result::set)
                .error(error::set).then(new Promise<ChatInfo>((resolve, errorHandler) -> {
                    if (error.get() != null) {
                        errorHandler.handle(error.get());
                        return;
                    }
                    var response = result.get();
                    var body = response.getBody();
                    if (!response.getSuccess()) {
                        errorHandler.handle(new PromiseError(new FirebaseError(response.getCode(), (String) body.get("error"))));
                        return;
                    }
                    resolve.run(new ChatInfo(body));
                }));
    }

    /**
     * promise exception can be a Throwable or a FirebaseError
     *
     * @return promise of chat update
     */
    @SuppressWarnings("unchecked")
    private Promise<NewMessagesUpdate> updateMessages() {

        var app = SwiftlyApp.getInstance();
        var result = new AtomicReference<FirebaseResponse>();
        var error = new AtomicReference<PromiseError>();
        return (Promise<NewMessagesUpdate>) app.database.getAsync("chats/" + chatId + "/messages",
                RealtimeDatabase.query("orderBy", "\"timestamp\""),
                RealtimeDatabase.query("startAt", Long.toString(messages.size() > 0 ? messages.get(messages.size() - 1).getTimestamp() : 0)))
                .resolved(result::set)
                .error(error::set).then(
                        new Promise<NewMessagesUpdate>(((resolve, exception) -> {
                            if (error.get() != null) {
                                exception.handle(error.get());
                                return;
                            }
                            var response = result.get();
                            var body = response.getBody();
                            if (!response.getSuccess()) {
                                exception.handle(new PromiseError(new FirebaseError(response.getCode(), (String) body.get("error"))));
                                return;
                            }
                            if (response.getBody().size() == 0) {
                                LOGGER.warn("chat update returned 0 new messages");
                                return;
                            }

                            var rawMessages = response.getBody().values().toArray();
                            var rawMessagesIds = response.getBody().keySet().toArray();
                            int newMessagesIndex = messages.size();
                            for (int i = 0; i < rawMessages.length; i++) {
                                var a = (Map<String, Object>) rawMessages[i];
                                var messageId = rawMessagesIds[i].toString();
                                var senderId = a.get("senderId").toString();
                                var content = a.get("content").toString();
                                var timestamp = Long.parseLong(a.get("timestamp").toString());
                                if (messages.stream().filter(m -> m.getMessageId().equals(messageId)).findAny().orElse(null) == null) {
                                    LOGGER.info("New message " + senderId + " " + content);
                                    var msg = new Message(messageId, senderId, content, timestamp);
                                    messages.add(msg);
                                }
                            }
                            messages.sort(Comparator.comparingLong(Message::getTimestamp));
                            resolve.run(new NewMessagesUpdate(this, newMessagesIndex));
                        }))
                );
    }


    public void updateLastCheck() {
        var app = SwiftlyApp.getInstance();
        String userId = (String) app.getUser().get("localId");
        String auth = (String) app.getUser().get("idToken");
        app.database.putAsync("/users/" + userId + "/chatsIndex/" + chatId + "/lastCheckTimestamp"
                , Map.of(".sv", "timestamp")
                , RealtimeDatabase.query("auth", auth)).resolved(response -> {

            if (!response.getSuccess()) {
                LOGGER.error(response.getBody().get("error"));
                LOGGER.error("Couldn't update user data!");
            }
        }).error(
                e -> {
                    e.getThrowable().printStackTrace();
                    LOGGER.error("Couldn't update user data!");
                }).dispatch();


    }

    public Promise<FirebaseResponse> sendMessage(String s) {
        var app = SwiftlyApp.getInstance();
        var payload = Map.of(
                "content", s,
                "senderId", app.getUser().get("localId"),
                "timestamp", Map.of(".sv", "timestamp")
        );
        return app.database.postAsync("/chats/" + chatId + "/messages/", payload);
    }

    @Override
    public void broadcastAll(ChatUpdate value) {
        listeners.forEach(l -> l.onReceive(value));
    }

    @Override
    public void addBroadcastListener(BroadcastListener<ChatUpdate> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeBroadcastListener(BroadcastListener<ChatUpdate> listener) {
        listeners.remove(listener);
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public String getChatId() {
        return chatId;
    }

    public ChatIndex getIndex() {
        return index;
    }

    public ChatInfo getChatInfo() {
        return this.chatInfo;
    }


    public String[] getParticipants() {
        return participants;
    }
}
