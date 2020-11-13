package swiftly.promise;

public interface PromiseResolve<T> {
    void run(T result);
}
