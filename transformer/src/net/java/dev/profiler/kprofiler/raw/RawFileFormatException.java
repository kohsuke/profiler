package net.java.dev.profiler.kprofiler.raw;

/**
 * Signals an error in the raw profiler output format.
 * @author Kohsuke Kawaguchi
 */
public class RawFileFormatException extends RuntimeException {
    public RawFileFormatException(String message) {
        super(message);
    }

    public RawFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public RawFileFormatException(Throwable cause) {
        super(cause);
    }
}
