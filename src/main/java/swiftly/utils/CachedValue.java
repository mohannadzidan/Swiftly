package swiftly.utils;

public class CachedValue<T> {
    private final T value;
    private long refreshTime;
    private final long lifetime;

    public CachedValue(T value, long lifetime){
        this.value = value;
        this.lifetime = lifetime;
        refresh();
    }

    public T get(){
        return value;
    }

    public boolean isDead(){
        return refreshTime + lifetime < System.currentTimeMillis();
    }

    public void refresh(){
        refreshTime = System.currentTimeMillis();
    }
}
