package net.java.dev.profiler.kprofiler.util;

import java.util.Iterator;

/**
 * {@link Iterator} that iterates a portion of an array.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class ArrayIterator<V> implements Iterator<V> {
    private final V[] data;
    private final int max;
    private int current;

    public ArrayIterator(V[] data, int start, int max ) {
        this.data = data;
        this.current = start;
        this.max = max;
    }

    public boolean hasNext() {
        return current<max;
    }

    public V next() {
        return data[current++];
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
