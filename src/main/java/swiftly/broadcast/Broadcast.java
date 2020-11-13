package swiftly.broadcast;

public interface Broadcast<T extends BroadcastData<?>> {
    void broadcastAll(T value);
    void addBroadcastListener(BroadcastListener<T> listener);
    void removeBroadcastListener(BroadcastListener<T> listener);
}
