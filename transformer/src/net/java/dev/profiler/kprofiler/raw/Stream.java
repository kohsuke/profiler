package net.java.dev.profiler.kprofiler.raw;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Stream {

    protected final RawDataFile parent;
    private final RawDataFile.DataStream rawStream;
    protected final DataInputStream stream;
    private final String name;

    Stream(RawDataFile parent,int id) throws IOException {
        this.parent = parent;
        rawStream = parent.getStream(id);
        this.stream = new DataInputStream(rawStream);
        this.name = parent.properties.get(".StreamName-"+id);
    }

    /**
     * Gets the name of the stream.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the number of bytes in this stream.
     */
    public int getSize() {
        return rawStream.totalSize();
    }

    /**
     * Gets the number of bytes consumed.
     */
    public int getCurrentPos() {
        return rawStream.bytesRead();
    }
}
