package net.java.dev.profiler.kprofiler.raw;

import net.java.dev.profiler.kprofiler.util.ClosedHash;
import net.java.dev.profiler.kprofiler.util.IotaGen;

import java.io.DataInput;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public final class MethodInfo extends ClosedHash.Entry {
    public final ClassInfo parent;

    public final String name;
    public final String signature;

    /**
     * Unique index number of this ClassInfo.
     * <p>
     * Unlikes IDs, index numbers are continuous.
     */
    public final int index;

    /**
     * Children of the same {@link ClassInfo} are linked by this field.
     */
    public MethodInfo nextSibling;

    /**
     * All {@link MethodInfo}s are linked by this field.
     */
    public MethodInfo nextMethod;

    public MethodInfo(ClassInfo parent,IotaGen<MethodInfo> index,DataInput di) throws IOException {
        super(di.readInt());
        this.parent = parent;

        MethodInfo last = index.getLast();
        if(last!=null)
            last.nextMethod = this;
        this.index = index.next(this);

        name = di.readUTF();
        signature = di.readUTF();
    }

    /**
     * Returns the fully qualified method name.
     */
    public String fullName() {
        return parent.name+'.'+name;
    }
}
