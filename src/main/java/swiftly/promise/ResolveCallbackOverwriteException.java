package swiftly.promise;

public class ResolveCallbackOverwriteException extends RuntimeException {
    public ResolveCallbackOverwriteException(String msg){
        super(msg);
    }
    public ResolveCallbackOverwriteException(){
        super("The resolve callback will be replaced, the function (then(PromiseSolution<T>) is expected to be called only once.");
    }
}
