package net.java.dev.profiler.kprofiler.raw;

import net.java.dev.profiler.kprofiler.util.IotaGen;

import java.io.IOException;
import java.util.Iterator;

/**
 * A {@link Stream} that captures the method/class definitions.
 *
 * <p>
 * Normally, the ap
 *
 * @author Kohsuke Kawaguchi
 */
public final class ClassStream extends Stream implements Iterator<ClassInfo> {

    private ClassInfo next;
    private boolean eof;

    private final IotaGen<ClassInfo> classIndex = new IotaGen<ClassInfo>();
    private final IotaGen<MethodInfo> methodIndex = new IotaGen<MethodInfo>();

    /**
     * {@link ClassInfo}s that are parsed so far.
     */
    public final Iterable<ClassInfo> classes = new Iterable<ClassInfo>() {
        public Iterator<ClassInfo> iterator() {
            return new Iterator<ClassInfo>() {
                private ClassInfo next = classIndex.getFirst();
                public boolean hasNext() {
                    return next!=null;
                }

                public ClassInfo next() {
                    ClassInfo r = next;
                    next = next.nextClass;
                    return r;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    };

    /**
     * {@link MethodInfo}s that are parsed so far.
     */
    public final Iterable<MethodInfo> methods = new Iterable<MethodInfo>() {
        public Iterator<MethodInfo> iterator() {
            return new Iterator<MethodInfo>() {
                private MethodInfo next = methodIndex.getFirst();
                public boolean hasNext() {
                    return next!=null;
                }

                public MethodInfo next() {
                    MethodInfo r = next;
                    next = next.nextMethod;
                    return r;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    };


    ClassStream(RawDataFile parent) throws IOException {
        super(parent,2);
    }


    public boolean hasNext() {
        if(next==null && !eof) {
            try {
                // try to fetch the new record
                int header = stream.read();
                if(header!='C') {
                    eof = true;
                    return false;
                }

                next = new ClassInfo(stream,classIndex,methodIndex);
            } catch (IOException e) {
                throw new RawFileFormatException(e);
            }
        }
        return next!=null;
    }

    public ClassInfo next() {
        if(next==null)
            throw new IllegalStateException();
        ClassInfo r = next;
        next = null;
        return r;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
