package net.java.dev.profiler.kprofiler.raw;

import net.java.dev.profiler.kprofiler.util.IotaGen;

import java.io.DataInput;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
public final class ClassInfo implements Iterable<MethodInfo> {
    /**
     * Unique index number of this ClassInfo.
     * <p>
     * Unlikes IDs, index numbers are continuous.
     */
    public final int index;

    /**
     * Class ID.
     */
    public final int id;
    /**
     * Fully-qualified class name.
     */
    public final String name;

    /**
     * Where was this class loaded from?
     */
    public final String source;

    /**
     * Child {@link MethodInfo}s are linked.
     */
    private MethodInfo firstChild;

    /**
     * All {@link ClassInfo}s are linked in the order of their index.
     */
    public ClassInfo nextClass;


    public Iterator<MethodInfo> iterator() {
        return new Iterator<MethodInfo>() {
            private MethodInfo next = firstChild;
            public boolean hasNext() {
                return next!=null;
            }
            public MethodInfo next() {
                MethodInfo r = next;
                next = next.nextSibling;
                return r;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private void addChild(MethodInfo mi) {
        mi.nextSibling = firstChild;
        firstChild = mi;
    }

    public MethodInfo getFirstChild() {
        return firstChild;
    }

    public ClassInfo( DataInput di, IotaGen<ClassInfo> classIndex, IotaGen<MethodInfo> methodIndex ) throws IOException {
        ClassInfo last = classIndex.getLast();
        if(last!=null)
            last.nextClass = this;
        this.index = classIndex.next(this);

        id = di.readInt();
        name = di.readUTF();
        source = di.readUTF();
        while(true) {
            char ch = (char)di.readByte();
            if(ch==0)
                return;
            addChild(new MethodInfo(this,methodIndex,di));
        }
    }
}
