package net.java.dev.profiler.kprofiler.impl;

import net.java.dev.profiler.kprofiler.MethodCall;

import java.util.Iterator;

/**
 * Iterates child {@link MethodCall}s of a {@link MethodCall}.
 * @author Kohsuke Kawaguchi
 */
public class MethodCallIterator implements Iterator<MethodCall> {
    private MethodCall next;

    public MethodCallIterator(MethodCall methodCall) {
        next = methodCall.getFirstChild();
    }

    public boolean hasNext() {
        return next!=null;
    }

    public MethodCall next() {
        MethodCall r = next;
        next = next.getNextSibling();
        return r;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
