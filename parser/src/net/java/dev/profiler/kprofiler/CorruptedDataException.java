package net.java.dev.profiler.kprofiler;

/**
 * Signals a corruption in the data file.
 * 
 * @author Kohsuke Kawaguchi
 */
public class CorruptedDataException extends RuntimeException {
    public CorruptedDataException(String message) {
        super(message);
    }

    public CorruptedDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public CorruptedDataException(Throwable cause) {
        super(cause);
    }
}
