package net.java.dev.profiler.kprofiler.raw;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Each profiler output contains a stream called "general stream" which contains
 * a key/value pair.
 *
 * <p>
 * This stream doesn't have a name nor a size, so it doesn't derive from {@link Stream}.
 *
 * @author Kohsuke Kawaguchi
 */
final class GeneralStream implements Iterator<NamedRecord> {

    private final DataInputStream stream;

    private NamedRecord next;
    private boolean eof;

    GeneralStream(RawDataFile parent) throws IOException {
        this.stream = new DataInputStream(parent.getStream((short)1));
    }

    public boolean hasNext() {
        if(next==null && !eof) {
            try {
                // try to fetch the new record
                int header = stream.read();
                if(header!='R') {
                    eof = true;
                    return false;
                }

                String name = stream.readUTF();
                String data = stream.readUTF();

                next = new NamedRecord(name,data);
            } catch (IOException e) {
                throw new RawFileFormatException(e);
            }
        }
        return next!=null;
    }

    public NamedRecord next() {
        if(next==null)
            throw new IllegalStateException();
        NamedRecord r = next;
        next = null;
        return r;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
