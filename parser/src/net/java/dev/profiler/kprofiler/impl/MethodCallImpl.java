package net.java.dev.profiler.kprofiler.impl;

import net.java.dev.profiler.kprofiler.MethodCall;
import net.java.dev.profiler.kprofiler.MethodInfo;

import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
final class MethodCallImpl extends Structure implements MethodCall, Iterable<MethodCall> {

    public MethodCallImpl(ProfileDataImpl owner, int index) {
        super(owner,index);
    }

    protected int getInt(int offset) {
        assert 0<=offset && offset<8;
        return owner.callTree.get(index*8+offset);     // one record = 8 int
    }




    public int callCount() {
        return getInt(0);
    }

    public long time() {
        return getLong(1);
    }

    public MethodInfo method() {
        return owner.getMethodInfo(getInt(3));
    }

    public MethodCall getNextByMethod() {
        return getMethodCall(4);
    }

    public MethodCall getParent() {
        return getMethodCall(5);
    }

    public MethodCall getFirstChild() {
        return getMethodCall(6);
    }

    public MethodCall getNextSibling() {
        return getMethodCall(7);
    }

    public Iterable<MethodCall> children() {
        return this;
    }

    public Iterator<MethodCall> iterator() {
        return new MethodCallIterator(this);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (this.getClass()!=o.getClass())  return false;

        final Structure that = (Structure) o;

        return this.index==that.index && this.owner==that.owner;
    }

    public int hashCode() {
        return index;
    }

}
