package swiftly.promise;

public class BadPromiseChainException extends RuntimeException {

    public BadPromiseChainException(String msg){
        super(msg);
    }
}
