package swiftly.promise;

public interface PromiseDispatch<T> {
    void run(PromiseResolve<T> resolve, PromiseErrorHandler errorHandler);
}
