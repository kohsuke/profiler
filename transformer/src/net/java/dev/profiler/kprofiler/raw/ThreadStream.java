package net.java.dev.profiler.kprofiler.raw;

import java.io.IOException;
import java.io.EOFException;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Stream} that captures the method enter/exit events.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ThreadStream extends Stream implements Iterator<RawMethodTrace> {

    private RawMethodTrace next;
    private boolean eof;

    private final List<RawMethodEnter> enterCache = new ArrayList<RawMethodEnter>();
    private final List<RawMethodLeave> leaveCache = new ArrayList<RawMethodLeave>();

    private int depth = 0;

    ThreadStream(RawDataFile parent,int id) throws IOException {
        super(parent,id);
    }

    public boolean hasNext() {
        if(next==null && !eof) {
            try {
                // try to fetch the new record
                int i = stream.readInt();
                if(i<0) {
                    RawMethodEnter e;
                    if(enterCache.size()==depth) {
                        e = new RawMethodEnter();
                        enterCache.add(e);
                        leaveCache.add(new RawMethodLeave());
                    } else {
                        e = enterCache.get(depth);
                    }
                    e.methodId = i&0x7FFFFFFF;
                    next = e;
                    depth++;
                } else {
                    depth--;

                    long time = i;
                    if((time&0x40000000)!=0) {
                        time = (time&0x3FFFFFFF)<<32;
                        time |= ((long)stream.readInt())&0xFFFFFFFFL;
                    }
                    if(time==0)
                        throw new RawFileFormatException("0 duration");
                    if(time<0)
                        throw new RawFileFormatException("Negative duration");

                    RawMethodLeave l = leaveCache.get(depth);
                    l.methodId = enterCache.get(depth).methodId;
                    l.time = time;
                    next = l;
                }
            } catch (EOFException e) {
                eof = true;
            } catch (IOException e) {
                throw new RawFileFormatException(e);
            }
        }
        return next!=null;
    }

    public RawMethodTrace next() {
        if(next==null)
            throw new IllegalStateException();
        RawMethodTrace r = next;
        next = null;
        return r;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
