package net.java.dev.profiler.kprofiler.raw;

/**
 * @author Kohsuke Kawaguchi
 */
public class RawMethodLeave extends RawMethodTrace {
    /**
     * The number of 'tick's spent in this method.
     */
    public long time;
}
