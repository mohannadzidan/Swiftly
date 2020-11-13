package swiftly.broadcast;

/**
 *
 * @param <T> broadcast data type
 * @param <K> event type
 */
public interface BroadcastListener<T extends BroadcastData<?>> {
    void onReceive(T broadcastData);
}
