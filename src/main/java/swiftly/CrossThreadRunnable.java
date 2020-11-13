package swiftly;

public interface CrossThreadRunnable {
    void runInAnotherThread(Runnable runnable);
}
