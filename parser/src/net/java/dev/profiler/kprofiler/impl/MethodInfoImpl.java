package net.java.dev.profiler.kprofiler.impl;

import net.java.dev.profiler.kprofiler.MethodInfo;
import net.java.dev.profiler.kprofiler.ClassInfo;
import net.java.dev.profiler.kprofiler.MethodCall;
import net.java.dev.profiler.kprofiler.Format;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
final class MethodInfoImpl extends Structure implements MethodInfo, Iterable<MethodCall> {
    public MethodInfoImpl(ProfileDataImpl owner, int index) {
        super(owner, index);
    }

    protected int getInt(int offset) {
        return owner.methodTable.get(index*5+offset);     // one record = 4 int
    }

    public ClassInfo getClazz() {
        return owner.getClassInfo(getInt(0));
    }

    public String name() {
        return getString(1);
    }

    public String signature() {
        return getString(2);
    }

    public MethodInfoImpl getNextSibling() {
        return owner.getMethodInfo(getInt(3));
    }

    public MethodCall firstCall() {
        return getMethodCall(4);
    }

    public Iterable<MethodCall> calls() {
        return this;
    }

    public Iterator<MethodCall> iterator() {
        return new Iterator<MethodCall>() {
            private MethodCall next = firstCall();

            public boolean hasNext() {
                return next!=null;
            }

            public MethodCall next() {
                MethodCall r = next;
                next = next.getNextByMethod();
                return r;
            }

            public void remove() {
            }
        };
    }

    public String fullName() {
        return getClazz().name()+'.'+name();
    }

    public String returnType(Format format) {
        String sig = signature();
        int idx = sig.lastIndexOf(')');
        sig = sig.substring(idx+1);

        return format.format(sig).typeName;
    }

    public String[] parameterTypes(Format format) {
        String sig = signature().substring(1);  // skip the first '('
        List<String> params = new ArrayList<String>();

        while(sig.charAt(0)!=')') {
            Format.Result r = format.format(sig);
            params.add(r.typeName);
            sig = r.restOfSig;
        }

        return params.toArray(new String[params.size()]);
    }
}
