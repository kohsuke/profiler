package net.java.dev.profiler.kprofiler.util;

/**
 * Index number generator.
 *
 * @author Kohsuke Kawaguchi
 */
public final class IotaGen<T> {
    private int index = 0;
    private T first;
    private T last;


    public int next(T owner) {
        if(first==null)
            first = owner;
        last = owner;
        return index++;
    }

    public T getFirst() {
        return first;
    }

    public T getLast() {
        return last;
    }
}
