package net.java.dev.profiler.kprofiler.raw;

import net.java.dev.profiler.kprofiler.util.ArrayIterator;
import net.java.dev.profiler.kprofiler.util.ClosedHash;
import net.java.dev.profiler.kprofiler.util.SingletonIterator;
import net.java.dev.profiler.kprofiler.util.IotaGen;

import java.util.Collections;
import java.util.Iterator;

/**
 * Method call.
 *
 * This object forms a tree structure.
 *
 * <p>
 * {@link ClosedHash.Entry#id} is the method ID.
 *
 * @author Kohsuke Kawaguchi
 */
public final class MethodCall extends ClosedHash.Entry implements Iterable<MethodCall> {
    /**
     * Unique index number of this ClassInfo.
     * <p>
     * Unlikes IDs, index numbers are continuous.
     */
    public final int index;

    /**
     * How many times does this method called from its parent?
     */
    public int callCount;

    /**
     * timer ticks.
     */
    public long time;

    /**
     * {@link MethodCall}s that share the same method {@link #id} are linked
     * in one list using this pointer.
     */
    public MethodCall nextByMethod;

    /**
     * All {@link MethodCall}s are linked in the order of their index.
     */
    public MethodCall nextCall;

    public final MethodCall parent;

    public MethodCall(MethodCall parent, int methodId, IotaGen<MethodCall> indexGen) {
        super(methodId);
        this.parent = parent;

        MethodCall last = indexGen.getLast();
        if(last!=null)
            last.nextCall = this;
        index = indexGen.next(this);
    }

    public long nanoTime(RawDataFile file) {
        long r = (long) (((double) time) / file.counterFrequency * 1000000000);
        if(r<0)
            throw new IllegalStateException();
        return r;
//        return time * 1000000000L / file.counterFrequency;
    }

    public long microTime(RawDataFile file) {
        return time*1000000 / file.counterFrequency;
    }

    /**
     * Child {@link MethodCall}s.
     *
     * <p>
     * If there's no child, this field is null.
     *
     * <p>
     * If there's just 1, this field directly points to
     * the child {@link MethodCall}.
     *
     * <p>
     * For children up to 8, we use an array and perform
     * a linear search.
     *
     * <p>
     * For more than that, we use {@link ClosedHash}.
     */
    private Object children;

    /**
     * Gets the definition of the method.
     *
     * @param dic
     *      Because there will be a lot of {@link MethodCall} instances,
     *      keeping the reference to the dictionary in every single object
     *      will be expensive. So the caller needs to pass it.
     */
    public MethodInfo getMethod(MethodInfoDictionary dic) {
        if(id==0)   return null;
        else        return dic.methods.get(id);
    }

    /**
     * Gets or creates the {@link MethodCall} of the given ID.
     */
    public MethodCall get(int id, IotaGen<MethodCall> indexGen) {
        if(children==null) {
            MethodCall child = new MethodCall(this,id,indexGen);
            children = child;
            return child;
        }
        if (children instanceof MethodCall) {
            MethodCall child = (MethodCall) children;
            if(child.id==id)
                return child;

            MethodCall[] list = new MethodCall[8];
            list[0] = child;
            list[1] = new MethodCall(this,id,indexGen);
            children = list;
            return list[1];
        }
        if (children instanceof MethodCall[]) {
            MethodCall[] list = (MethodCall[]) children;
            int len = list.length;
            int i;
            for( i=0; i<len; i++ ) {
                MethodCall item = list[i];
                if( item==null )
                    break;
                if( item.id==id )
                    return list[i];
            }

            if(i<len) {
                return list[i] = new MethodCall(this,id,indexGen);
            }

            ClosedHash<MethodCall> hash = new ClosedHash<MethodCall>();
            this.children = hash;

            for( i=0; i<len; i++ )
                hash.put(list[i]);

            MethodCall c = new MethodCall(this,id,indexGen);
            hash.put(c);

            return c;
        }
        if (children instanceof ClosedHash) {
            ClosedHash<MethodCall> hash = (ClosedHash<MethodCall>) children;

            MethodCall mc = hash.get(id);
            if(mc!=null)    return mc;

            mc = new MethodCall(this,id,indexGen);
            hash.put(mc);
            return mc;
        }
        throw new IllegalStateException();
    }

    public Iterator<MethodCall> iterator() {
        if(children==null) {
            return Collections.<MethodCall>emptyList().iterator();
        }
        if (children instanceof MethodCall) {
            return new SingletonIterator<MethodCall>((MethodCall) children);
        }
        if (children instanceof MethodCall[]) {
            MethodCall[] list = (MethodCall[]) children;
            int len = list.length;
            int i;
            for( i=0; i<len; i++ ) {
                MethodCall item = list[i];
                if( item==null )
                    break;
            }

            return new ArrayIterator<MethodCall>(list,0,i);
        }
        if (children instanceof ClosedHash) {
            ClosedHash<MethodCall> hash = (ClosedHash<MethodCall>) children;
            return hash.iterator();
        }
        throw new IllegalStateException();
    }
}
