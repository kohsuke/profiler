package net.java.dev.profiler.kprofiler;

/**
 * Receives notifications periodically while the transformation is in progress.
 *
 * @author Kohsuke Kawaguchi
 */
public interface ProgressMonitor {
    /**
     *
     * @param total
     *      always the size of the stream.
     */
    void progress( int current, int total );

    /**
     * Internally used.
     *
     * The progress notification is sent for every {@link #FREQUENCY} items.
     */
    static final int FREQUENCY = 1024;
}
