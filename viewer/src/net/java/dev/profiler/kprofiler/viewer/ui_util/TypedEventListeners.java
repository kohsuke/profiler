package net.java.dev.profiler.kprofiler.viewer.ui_util;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * Maintains a list of {@link EventListener}s that provides a proxy instance to call
 * listeners without writing one method per each kind of callback.
 *
 * <p>
 * This class also maintains listeners in a weak reference to avoid memory leak.
 *
 * @author Kohsuke Kawaguchi
 */
public class TypedEventListeners<T extends EventListener> implements InvocationHandler {
    private final T sink;

    private final List<WeakReference<T>> listeners = new ArrayList<WeakReference<T>>();

    public TypedEventListeners(Class<T> type) {
        sink = (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},this);
    }

    /**
     * Calling a method on this interface will invoke all the listeners.
     */
    public T getSink() {
        return sink;
    }

    public void add(T t) {
        listeners.add(new WeakReference<T>(t));
    }

    public void remove(T t) {
        for( WeakReference<T> ref : listeners )
            if(ref.get()==t) {
                listeners.remove(ref);
                return;
            }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            for( int i=listeners.size()-1; i>=0; i-- ) {
                T t = listeners.get(i).get();
                if(t==null) {
                    listeners.remove(i);
                } else {
                    method.invoke(t,args);
                }
            }
            return null;
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if(t instanceof Error)
                throw (Error)t;
            if(t instanceof RuntimeException)
                throw (RuntimeException)t;
            // this shouldn't be possible
            throw new Error(t);
        }
    }
}
