
import net.java.dev.profiler.kprofiler.raw.ClassInfo;
import net.java.dev.profiler.kprofiler.raw.MethodInfo;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
public class DumpClass extends Dumper {
    public static void main(String[] args) throws IOException {
        new DumpClass(new File(args[0])).main();
    }

    public DumpClass(File file) throws IOException {
        super(file);
    }

    private void main() throws IOException {
        Iterator<ClassInfo> cs = rd.getClassStream();
        while(cs.hasNext()) {
            ClassInfo ci = cs.next();
            System.out.printf("%08X %s (%s)\n",ci.id,ci.name,ci.source);
            for( MethodInfo mi : ci )
                System.out.printf("  %08X %s %s\n",mi.id,mi.name,mi.signature);
        }
    }
}
