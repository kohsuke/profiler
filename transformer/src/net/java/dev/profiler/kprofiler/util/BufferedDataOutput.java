package net.java.dev.profiler.kprofiler.util;

import java.io.OutputStream;
import java.io.IOException;
import java.io.DataOutput;

/**
 * Buffering for {@link DataOutput}.
 * @author Kohsuke Kawaguchi
 */
public class BufferedDataOutput extends OutputStream {
    private final byte[] buf = new byte[8192];
    private int size = 0;

    private final DataOutput base;

    public BufferedDataOutput(DataOutput base) {
        this.base = base;
    }

    public void write(int b) throws IOException {
        if(buf.length==size)
            flush();
        buf[size++] = (byte)b;
    }

    public void write(byte b[], int off, int len) throws IOException {
        if(size+len<=buf.length) {
            System.arraycopy(b,off,buf,size,len);
            size += len;
        } else {
            flush();
            base.write(b,off,len);
        }
    }

    public void flush() throws IOException {
        base.write(buf,0,size);
        size = 0;
    }

    public void close() throws IOException {
    }
}
