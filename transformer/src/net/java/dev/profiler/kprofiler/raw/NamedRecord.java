package net.java.dev.profiler.kprofiler.raw;

/**
 * Named data.
 *
 * @author Kohsuke Kawaguchi
 */
public final class NamedRecord {
    /**
     * Name of this record. Always non-null.
     */
    public final String name;
    /**
     * Data.
     */
    public final String data;

    public NamedRecord(String name, String data) {
        this.name = name;
        this.data = data;
    }
}
