package net.java.dev.profiler.kprofiler.util;

import java.util.Iterator;

/**
 * {@link Iterator} that returns a single object.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SingletonIterator<V> implements Iterator<V> {
    private boolean seen = false;
    private final V value;

    public SingletonIterator(V value) {
        this.value = value;
    }

    public boolean hasNext() {
        return !seen;
    }

    public V next() {
        seen = true;
        return value;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
