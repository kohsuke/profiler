package net.java.dev.profiler.kprofiler.transformer;

import net.java.dev.profiler.kprofiler.ProgressMonitor;
import net.java.dev.profiler.kprofiler.raw.ClassInfo;
import net.java.dev.profiler.kprofiler.raw.MethodCall;
import net.java.dev.profiler.kprofiler.raw.MethodInfo;
import net.java.dev.profiler.kprofiler.raw.MethodInfoDictionary;
import net.java.dev.profiler.kprofiler.raw.RawFileFormatException;
import net.java.dev.profiler.kprofiler.raw.RawMethodEnter;
import net.java.dev.profiler.kprofiler.raw.RawMethodLeave;
import net.java.dev.profiler.kprofiler.raw.RawMethodTrace;
import net.java.dev.profiler.kprofiler.raw.ThreadStream;
import net.java.dev.profiler.kprofiler.util.BufferedDataOutput;
import net.java.dev.profiler.kprofiler.util.IotaGen;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UTFDataFormatException;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Transforms the raw {@link ThreadStream}s into more compact
 * accumulated call tree form.
 *
 * @author Kohsuke Kawaguchi
 */
public class Transformer {
    private ProgressMonitor monitor;

    /**
     * Used to assign indices to {@link MethodCall}s.
     */
    private final IotaGen<MethodCall> indexGen = new IotaGen<MethodCall>();

    private final MethodCall root = new MethodCall(null,0,indexGen);

    /**
     * Tails of lists of {@link MethodCall}s that share the same method ID.
     * This array is keyed by the {@link MethodInfo#index}.
     */
    private final MethodCall[] tails;
    private final MethodCall[] heads;


    private final MethodInfoDictionary dic;

    public Transformer(MethodInfoDictionary dic) {
        this.dic = dic;
        heads = new MethodCall[dic.countMethods()];
        tails = new MethodCall[dic.countMethods()];
    }

    public ProgressMonitor getMonitor() {
        return monitor;
    }

    public void setMonitor(ProgressMonitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Adds the call trace of the given thread stream into this accumulated
     * call tree form.
     */
    public void transform( ThreadStream raw ) {
        MethodCall[] callTree = new MethodCall[256];

        callTree[0] = root;
        int top=1;
        int count = 0;
        int total = raw.getSize();

        while(raw.hasNext()) {
            RawMethodTrace mc = raw.next();
            if(mc instanceof RawMethodEnter) {
                MethodCall child = callTree[top - 1].get(mc.methodId,indexGen);
                if(child.callCount==0) {
                    // add this to the link
                    int mindex = mc.getMethod(dic).index;
                    MethodCall tail = tails[mindex];
                    tails[mindex] = child;
                    if(tail!=null)
                        tail.nextByMethod = child;
                    else
                        heads[mindex] = child;
                }
                child.callCount++;

                callTree[top] = child;
                top++;
                if(top==callTree.length) {
                    // reallocate
                    MethodCall[] c = new MethodCall[top*2];
                    System.arraycopy(callTree,0,c,0,top);
                    callTree = c;
                }
            } else {
                RawMethodLeave rml = (RawMethodLeave)mc;
                top--;

                long old = callTree[top].time;
                callTree[top].time += rml.time;
                if( old > callTree[top].time )
                    throw new RawFileFormatException("Call time overflow");

                if(callTree[top].id!=mc.methodId) {
                    MethodInfo enter = callTree[top].getMethod(dic);
                    String enterName = enter!=null ? enter.fullName() : "(none)";

                    MethodInfo leave = mc.getMethod(dic);
                    String leaveName = leave!=null ? leave.fullName() : "(noone)";
                    throw new RawFileFormatException("Inconsistent enter/leave nesting"+
                            " enter: "+enterName+" leave: "+leaveName);
                }
            }

            count++;
            if((count%ProgressMonitor.FREQUENCY)==0 && monitor!=null)
                monitor.progress(raw.getCurrentPos(),total);
        }

        if(top!=1)
            throw new RawFileFormatException("Missing "+(top-1)+" leave methods");

        // set the duration of the root method
        root.time = 0;
        for( MethodCall mc : root )
            root.time += mc.time;
    }

    /**
     * Gets the computed accumulated call tree form.
     */
    public MethodCall getRoot() {
        return root;
    }

    public int countMethodCalls() {
        MethodCall last = indexGen.getLast();
        if(last==null)  return 0;
        else            return last.index+1;
    }

    private static class ConstantPool {
        private final OutputStream file;
        private final int start;

        // TODO: efficiency
        Map<String,Integer> index = new HashMap<String,Integer>();
        int length = 0;

        /**
         * Buffer.
         */
        private byte[] bytearr = new byte[256];

        public ConstantPool(OutputStream io, int start) {
            this.file = io;
            this.start = start;
        }


        int write(String str) throws IOException {
            Integer i = index.get(str);
            if(i==null) {
                i = length;
                index.put(str,i);
                length += writeUTF(str);
            }
            return i;
        }

        private int writeUTF(String str) throws IOException {
            int strlen = str.length();
            int utflen = 0;
            int c, count = 0;

            /* use charAt instead of copying String to char array */
            for (int i = 0; i < strlen; i++) {
                c = str.charAt(i);
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    utflen++;
                } else if (c > 0x07FF) {
                    utflen += 3;
                } else {
                    utflen += 2;
                }
            }

            if (utflen > 65535)
                throw new UTFDataFormatException("encoded string too long: " + utflen + " bytes");

            if (bytearr.length < (utflen + 2))
                bytearr = new byte[(utflen * 2) + 2];

            bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
            bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);

            int i = 0;
            for (i = 0; i < strlen; i++) {
                c = str.charAt(i);
                if (!((c >= 0x0001) && (c <= 0x007F))) break;
                bytearr[count++] = (byte) c;
            }

            for (; i < strlen; i++) {
                c = str.charAt(i);
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    bytearr[count++] = (byte) c;

                } else if (c > 0x07FF) {
                    bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                    bytearr[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                    bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
                } else {
                    bytearr[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                    bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
                }
            }

            file.write(bytearr, 0, utflen+2);

            return utflen + 2;
        }
    }
    /**
     * Writes to the specified file.
     */
    public void write( File file ) throws IOException {
        RandomAccessFile data = new RandomAccessFile(file,"rw");
        data.writeInt(0xDeadBeef);      // signature
        data.writeInt(0);               // start of class table
        data.writeInt(0);               // start of method table
        data.writeInt(0);               // start of call tree

        DataOutputStream bufData = new DataOutputStream(new BufferedDataOutput(data));
        ConstantPool pool = new ConstantPool(bufData,16);

        // fill in constant pools
        for( ClassInfo c : dic.allClasses ) {
            pool.write(c.name);
        }
        for( MethodInfo m : dic.allMethods ) {
            pool.write(m.name);
            pool.write(m.signature);
        }
        bufData.flush();

        // write class tables
        long classTableStart = data.getFilePointer();
        for( ClassInfo c : dic.allClasses ) {
            bufData.writeInt(pool.write(c.name));
            MethodInfo firstChild = c.getFirstChild();
            if(firstChild==null)
                bufData.writeInt(-1);
            else
                bufData.writeInt(firstChild.index);
        }
        bufData.flush();

        // write method tables
        long methodTableStart = data.getFilePointer();
        for( MethodInfo m : dic.allMethods ) {
            bufData.writeInt(m.parent.index);
            bufData.writeInt(pool.write(m.name));
            bufData.writeInt(pool.write(m.signature));
            if(m.nextSibling!=null)
                bufData.writeInt(m.nextSibling.index);
            else
                bufData.writeInt(-1);
            if(heads[m.index]==null)
                bufData.writeInt(-1);
            else
                bufData.writeInt(heads[m.index].index);
        }
        bufData.flush();

        // build sibling method call tables
        int[] nextSibling = new int[countMethodCalls()];
        Arrays.fill(nextSibling,-1);
        for( MethodCall mc=indexGen.getFirst(); mc!=null; mc=mc.nextCall ) {
            MethodCall prev = null;
            for( MethodCall child : mc ) {
                if(prev!=null)
                    nextSibling[prev.index] = child.index;
                prev = child;
            }
        }

        // write call trace
        long callTraceStart = data.getFilePointer();
        for( MethodCall mc=indexGen.getFirst(); mc!=null; mc=mc.nextCall ) {
            bufData.writeInt(mc.callCount);
            bufData.writeLong(mc.nanoTime(dic.parent));
            if(mc.id==0)
                bufData.writeInt(-1);
            else
                bufData.writeInt(mc.getMethod(dic).index);
            if(mc.nextByMethod==null)
                bufData.writeInt(-1);
            else
                bufData.writeInt(mc.nextByMethod.index);
            if(mc.parent==null)
                bufData.writeInt(-1);
            else
                bufData.writeInt(mc.parent.index);

            int firstChild;
            Iterator<MethodCall> itr = mc.iterator();
            if(itr.hasNext())
                firstChild = itr.next().index;
            else
                firstChild = -1;
            bufData.writeInt(firstChild);
            bufData.writeInt(nextSibling[mc.index]);
        }
        bufData.flush();

        data.seek(4);
        data.writeInt((int)classTableStart);
        data.writeInt((int)methodTableStart);
        data.writeInt((int)callTraceStart);
    }

    /**
     * Choose the default output file name for the specified source.
     */
    public static File getDefaultOutput(File source) {
        File dest;
        String fileName = source.getName();
        int idx = fileName.indexOf('.');
        if(idx>0) // so that we don't handle ".foo"
            fileName = fileName.substring(0,idx)+".compact";
        dest = new File(source.getParentFile(),fileName);
        return dest;
    }
}
