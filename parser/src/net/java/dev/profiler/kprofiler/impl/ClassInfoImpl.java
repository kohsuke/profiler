package net.java.dev.profiler.kprofiler.impl;

import net.java.dev.profiler.kprofiler.ClassInfo;
import net.java.dev.profiler.kprofiler.MethodInfo;

import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
final class ClassInfoImpl extends Structure implements ClassInfo, Iterable<MethodInfo> {
    ClassInfoImpl(ProfileDataImpl owner, int index) {
        super(owner,index);
    }

    protected int getInt(int offset) {
        return owner.classTable.get(index*2+offset);     // one record = 2 int
    }

    public String name() {
        String name = getString(0);
        if(name.charAt(0)=='L') {
            name = name.substring(1,name.length()-1);
        }
        return name.replace('/','.');
    }

    public Iterable<MethodInfo> methods() {
        return this;
    }

    public MethodInfoImpl getFirstMethod() {
        return owner.getMethodInfo(getInt(1));
    }

    public Iterator<MethodInfo> iterator() {
        return new Iterator<MethodInfo>() {
            MethodInfoImpl next = getFirstMethod();
            public boolean hasNext() {
                return next!=null;
            }

            public MethodInfo next() {
                MethodInfoImpl r = next;
                next = next.getNextSibling();
                return r;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
