package net.java.dev.profiler.kprofiler;

/**
 * Information about a profiled class.
 * 
 * @author Kohsuke Kawaguchi
 */
public interface ClassInfo {
    /**
     * Gets the class name.
     */
    String name();

    /**
     * Returns all the methods in this class.
     */
    Iterable<MethodInfo> methods();
}
