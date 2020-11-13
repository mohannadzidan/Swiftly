package swiftly.broadcast;

/**
 *
 * @param <T> sender type
 */
public abstract class BroadcastData<T> {
    private final T sender;
    public BroadcastData(T sender){
        this.sender = sender;
    }
    public T getSender(){
        return sender;
    }
}
