package net.java.dev.profiler.kprofiler.raw;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Parser that reads the raw output from the profiler.
 *
 * @author Kohsuke Kawaguchi
 */
public final class RawDataFile {
    private final FileChannel channel;
    private final RandomAccessFile stream;

    private static final int SECTOR_SIZE = 64*1024;

    /**
     * How many clock ticks are there in one second?
     */
    public final long counterFrequency;

    /**
     * Contents of the general stream. Read-only.
     */
    public final Map<String,String> properties;

    private final ByteBuffer fat = ByteBuffer.allocateDirect(SECTOR_SIZE);

    public RawDataFile(File data) throws IOException {
        stream = new RandomAccessFile(data,"r");
        channel = stream.getChannel();

        channel.read(fat,0);

        if(fat.getShort(0)!=(short)0xBEEF)
            throw new RawFileFormatException("Not a raw profiler output");

        // read system properties
        Map<String, String> props = new HashMap<String, String>();
        Iterator<NamedRecord> gs = getGeneralStream();
        while(gs.hasNext()) {
            NamedRecord r = gs.next();
            props.put(r.name,r.data);
        }
        properties = Collections.unmodifiableMap(props);

        counterFrequency = Long.parseLong(properties.get(".counter.frequency"));
    }

    /**
     * Releases the associated system resources.
     * <p>
     * I think it's OK for this object to be just GC-ed without calling this method,
     * if the caller doesn't worry too much about timely release of resources.
     */
    public void close() throws IOException {
        channel.close();
        stream.close();
    }

    /**
     * Finds the nextByMethod sector number for the stream.
     */
    private int getNextSector(int current, int id) {
        int idx = current*2;
        while(idx<SECTOR_SIZE && fat.getShort(idx)!=id)
            idx += 2;

        if(idx==SECTOR_SIZE)
            return -1;      // end of stream
        return idx/2;
    }

    /**
     * Gets a raw data stream.
     */
    DataStream getStream(int id) throws IOException {
        return new DataStream(id);
    }

    /**
     * Gets a data stream.
     *
     * <p>
     * Multiple {@link ThreadStream}s for different/same IDs can be
     * opened at the same time.
     */
    public ThreadStream getThreadStream(int id) throws IOException {
        return new ThreadStream(this,id+3);
    }

    /**
     * Returns the number of thread streams.
     */
    public int countThreadStreams() {
        int i=3;
        String name;
        do {
            name = properties.get(".StreamName-"+i);
        } while(name!=null);
        return i-3;
    }

    public GeneralStream getGeneralStream() throws IOException {
        return new GeneralStream(this);
    }

    public ClassStream getClassStream() throws IOException {
        return new ClassStream(this);
    }

    final class DataStream extends InputStream {
        private int nextSector = 0;

        private final ByteBuffer buf2 = ByteBuffer.allocate(SECTOR_SIZE);
        private final byte[] buf = buf2.array();

        private int ptr = SECTOR_SIZE;

        private int sectorsRead = 0;

        private final int id;

        private final int totalSize;

        public DataStream(int id) throws IOException {
            this.id = id;
            if(id==1)
                totalSize = SECTOR_SIZE;
            else
                totalSize = Integer.parseInt(properties.get(".StreamSize-"+id));
            fetch();    // fetch the first sector
        }

        public int available() {
            return totalSize-bytesRead();
        }

        public int read() throws IOException {
            if(ptr==SECTOR_SIZE)
                fetch();
            if(available()==0)
                return -1;
            return buf[ptr++]&0xFF;
        }

        public int read(byte b[], int off, int len) throws IOException {
            int r = 0;

            while(len>0 && !isEOS()) {
                int sz = Math.min(len,SECTOR_SIZE-ptr);
                System.arraycopy(buf,ptr,b,off,sz);

                ptr += sz;
                r += sz;
                off += sz;
                len -= sz;

                fetch();
            }

            return r;
        }

        public long skip(long n) throws IOException {
            long r = 0;

            while(n>0 && !isEOS()) {
                int sz = Math.min((int)n,SECTOR_SIZE-ptr);
                n -= sz;
                ptr += sz;
                r += sz;
                fetch();
            }

            return r;
        }

        /**
         * Moves to the nextByMethod sector if necessary.
         */
        private void fetch() throws IOException {
            if(SECTOR_SIZE>ptr || nextSector==-1)
                return;

            nextSector = getNextSector(nextSector,id);
            if(nextSector!=-1) {
                buf2.position(0);
                int x = channel.read(buf2, nextSector * SECTOR_SIZE);
                if(x<SECTOR_SIZE)
                    throw new IOException("Unable to read fully");
                ptr = 0;
                nextSector++;
                sectorsRead++;
            }
        }

        private boolean isEOS() {
            return ptr==SECTOR_SIZE && nextSector==-1;
        }

        /**
         * Number of bytes that were read thus far.
         */
        int bytesRead() {
            return (sectorsRead-1)*SECTOR_SIZE+ptr;
        }

        int totalSize() {
            return totalSize;
        }
    }
}
