package swiftly.promise;

public class PromiseError {
    public enum Type{THROWABLE, ERROR}
    private final Throwable throwable;
    private final Object error;
    private final Type type;

    public PromiseError(Throwable throwable){
        if(throwable == null) throw new NullPointerException();
        this.throwable = throwable;
        this.error = null;
        this.type = Type.THROWABLE;
    }

    public PromiseError(Object error){
        if(error == null) throw new NullPointerException();
        this.error = error;
        this.throwable = null;
        this.type = Type.ERROR;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Object getError() {
        return error;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        if(type == Type.THROWABLE){
            return getThrowable().toString();
        }
        return getError().toString();
    }
}
