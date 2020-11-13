package swiftly.promise;


import org.apache.log4j.Logger;

import javax.lang.model.type.NullType;
import java.util.Collection;
import java.util.List;

/**
 * @param <T> promised value type
 */
public class Promise<T> {
    private final PromiseDispatch<T> onDispatch;
    private PromiseResolve<T> onResolve;
    private PromiseErrorHandler onException;
    private Promise<?> next, previous;

    public Promise(PromiseDispatch<T> onDispatch) {
        this.onDispatch = onDispatch;
    }

    /**
     * start resolving the promise
     */
    public void dispatch(){
        Promise<?> root = this;
        while(root.previous != null){
            root = root.previous;
        }
        root.dispatchInternally();
    }
    private void dispatchInternally(){
        onDispatch.run(this::resolve, getChainHandler());
    }
    public Promise<T> error(PromiseErrorHandler onException) {
        this.onException = onException;
        return this;
    }

    private PromiseErrorHandler getChainHandler(){
        Promise<?> end = this;
        PromiseErrorHandler nearestHandler = this.onException;
        while(end.next != null && nearestHandler == null){
            end = end.next;
            nearestHandler = end.onException;
        }
        return nearestHandler;
    }


    private void resolve(T result) {
        if(onResolve != null)onResolve.run(result);
        if(next != null) next.dispatchInternally();
    }

    public Promise<T> resolved(PromiseResolve<T> onResolve) {
        if(this.onResolve!=null) throw new ResolveCallbackOverwriteException();
        this.onResolve = onResolve;
        return this;
    }

    public Promise<?> then(Promise<?> next) {
        var nextRoot = getRoot(next);
        var thisEnd = getEnd(this);
        thisEnd.next = nextRoot;
        nextRoot.previous = thisEnd;
        return next;
    }
    public static Promise<?> getRoot(Promise<?> promise){
        var root = promise;
        while(root.previous != null){
            root = root.previous;
        }
        return root;
    }
    public static Promise<?> getEnd(Promise<?> promise){
        var end = promise;
        while(end.next != null){
            end = end.next;
        }
        return end;
    }
    public static Promise<NullType> all(List<Promise<?>> promises){
        Promise<NullType> root = new Promise<>((resolve, errorHandler) -> {
            resolve.run(null);
        });
        if(promises == null || promises.size() == 0) return root;
        var promise = promises.get(0);
        for(int i =1; i<promises.size(); i++){
            promise = promise.then(promises.get(i));
        }
        promise.then(root);
        return root;
    }
}
