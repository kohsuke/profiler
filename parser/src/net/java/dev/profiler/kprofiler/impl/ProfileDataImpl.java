package net.java.dev.profiler.kprofiler.impl;

import net.java.dev.profiler.kprofiler.ClassInfo;
import net.java.dev.profiler.kprofiler.CorruptedDataException;
import net.java.dev.profiler.kprofiler.MethodCall;
import net.java.dev.profiler.kprofiler.MethodInfo;
import net.java.dev.profiler.kprofiler.ProfileData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class ProfileDataImpl implements ProfileData {
    private final FileChannel channel;
    private final MappedByteBuffer view;
    private final FileInputStream stream;

    final ByteBuffer constantPool;
    final IntBuffer classTable;
    final IntBuffer methodTable;
    final IntBuffer callTree;

    /**
     * All {@link ClassInfo}s by their index.
     */
    final ClassInfoImpl[] classes;

    /**
     * View of {@link #classes}.
     */
    private final List<ClassInfoImpl> classList;

    /**
     * All {@link MethodInfo}s by their index.
     */
    final MethodInfoImpl[] methods;

    /**
     * View of {@link #methods}.
     */
    private final List<MethodInfoImpl> methodList;

    private final MethodCallImpl root = new MethodCallImpl(this,0);

    public ProfileDataImpl(File data) throws IOException {
        stream = new FileInputStream(data);
        channel = stream.getChannel();
        view = channel.map(FileChannel.MapMode.READ_ONLY,0,channel.size());
        int signature = view.getInt();
        if(signature!=0xDeadBeef)
            throw new IOException("Not a profiler data file");

        int classStart = view.getInt();
        int threadStart = view.getInt();
        int callStart = view.getInt();
        int size = (int)channel.size();

        constantPool = slice(view,view.position(),classStart);
        classTable = slice(view,classStart,threadStart).asIntBuffer();
        methodTable = slice(view,threadStart,callStart).asIntBuffer();
        callTree = slice(view,callStart,size).asIntBuffer();

        // fill in classes
        classes = new ClassInfoImpl[(threadStart-classStart)/8];
        classList = Arrays.asList(classes);
        for(int i=classes.length-1; i>=0; i--)
            classes[i] = new ClassInfoImpl(this,i);

        // fill in methods
        methods = new MethodInfoImpl[(callStart-threadStart)/20];
        methodList = Arrays.asList(methods);
        for(int i=methods.length-1; i>=0; i--)
            methods[i] = new MethodInfoImpl(this,i);

    }

    private static ByteBuffer slice(ByteBuffer view, int start, int end) {
        view.position(start);
        ByteBuffer child = view.slice();
        child.limit(end-start);
        return child.asReadOnlyBuffer();
    }

    public MethodCall getRoot() {
        return root;
    }

    public void close() throws IOException {
        channel.close();
        stream.close();
    }

    public Iterable<? extends ClassInfo> classes() {
        return classList;
    }

    public Iterable<? extends MethodInfo> methods() {
        return methodList;
    }

    public ClassInfo getClassInfo(int index) {
        return classes[index];
    }

    public MethodInfoImpl getMethodInfo(int index) {
        if(index==-1)
            return null;
        return methods[index];
    }


    // buffer used to parse constant pool.
    private byte[] bytearr = new byte[256];
    private char[] chararr = new char[256];

    /**
     * Reads a string from the constant pool.
     */
    public String getConstantPool(int offset) {
        // technically this has to be unsigned, not signed short.
        constantPool.position(offset);

        int utflen = constantPool.getShort();
        if (bytearr.length < utflen){
            bytearr = new byte[utflen*2];
            chararr = new char[utflen*2];
        }

        int c, char2, char3;
        int count = 0;
        int chararr_count=0;

        constantPool.get(bytearr,0,utflen);

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            if (c > 127) break;
            count++;
            chararr[chararr_count++]=(char)c;
        }

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            switch (c >> 4) {
                case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                    /* 0xxxxxxx*/
                    count++;
                    chararr[chararr_count++]=(char)c;
                    break;
                case 12: case 13:
                    /* 110x xxxx   10xx xxxx*/
                    count += 2;
                    if (count > utflen)
                        throw new CorruptedDataException(
                            "malformed input: partial character at end");
                    char2 = (int) bytearr[count-1];
                    if ((char2 & 0xC0) != 0x80)
                        throw new CorruptedDataException(
                            "malformed input around byte " + count);
                    chararr[chararr_count++]=(char)(((c & 0x1F) << 6) |
                                                    (char2 & 0x3F));
                    break;
                case 14:
                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
                    count += 3;
                    if (count > utflen)
                        throw new CorruptedDataException(
                            "malformed input: partial character at end");
                    char2 = (int) bytearr[count-2];
                    char3 = (int) bytearr[count-1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                        throw new CorruptedDataException(
                            "malformed input around byte " + (count-1));
                    chararr[chararr_count++]=(char)(((c     & 0x0F) << 12) |
                                                    ((char2 & 0x3F) << 6)  |
                                                    ((char3 & 0x3F) << 0));
                    break;
                default:
                    /* 10xx xxxx,  1111 xxxx */
                    throw new CorruptedDataException(
                        "malformed input around byte " + count);
            }
        }
        // The number of chars produced may be less than utflen
        return new String(chararr, 0, chararr_count);
    }
}
