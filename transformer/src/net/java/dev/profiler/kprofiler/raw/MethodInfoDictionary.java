package net.java.dev.profiler.kprofiler.raw;

import net.java.dev.profiler.kprofiler.util.ClosedHash;
import net.java.dev.profiler.kprofiler.ProgressMonitor;

import java.io.IOException;

/**
 * The parsed and indexed {@link MethodInfo}s.
 *
 * <p>
 * This data structure is necessary to parse {@link ThreadStream}s.
 *
 * @author Kohsuke Kawaguchi
 */
public final class MethodInfoDictionary {
    /**
     * Method definitions keyed by their IDs.
     */
    final ClosedHash<MethodInfo> methods = new ClosedHash<MethodInfo>(5000);

    public final RawDataFile parent;

    public final Iterable<MethodInfo> allMethods;
    public final Iterable<ClassInfo> allClasses;

    /**
     * Constructs this object.
     */
    public MethodInfoDictionary(RawDataFile parent,ProgressMonitor monitor) throws IOException {
        this.parent = parent;

        ClassStream cs = parent.getClassStream();

        int count = 0;
        int total = cs.getSize();

        while(cs.hasNext()) {
            for( MethodInfo mi : cs.next() ) {
                MethodInfo old = methods.put(mi);
                assert old==null;
            }

            count++;
            if((count%ProgressMonitor.FREQUENCY)==0 && monitor!=null)
                monitor.progress(cs.getCurrentPos(),total);
        }

        allMethods = cs.methods;
        allClasses = cs.classes;
    }

    public final int countMethods() {
        return methods.size();
    }
}
