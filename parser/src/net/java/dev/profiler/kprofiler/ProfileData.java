package net.java.dev.profiler.kprofiler;

import java.io.IOException;

/**
 * Parsed profiled result.
 *
 * <p>
 * Extends {@link Pointer} because this class points to the "root" method.
 *
 * @author Kohsuke Kawaguchi
 */
public interface ProfileData {
    /**
     * Gets the "root" method.
     */
    MethodCall getRoot();

    /**
     * Closes this data and releases associated system resources.
     */
    void close() throws IOException;

    /**
     * Iterates all the {@link ClassInfo}s in this data file.
     */
    Iterable<? extends ClassInfo> classes();

    /**
     * Iterates all the {@link MethodInfo}s in this data file.
     */
    Iterable<? extends MethodInfo> methods();
}
