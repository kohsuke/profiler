
import net.java.dev.profiler.kprofiler.raw.MethodInfo;
import net.java.dev.profiler.kprofiler.raw.MethodInfoDictionary;
import net.java.dev.profiler.kprofiler.raw.RawMethodEnter;
import net.java.dev.profiler.kprofiler.raw.RawMethodTrace;
import net.java.dev.profiler.kprofiler.raw.ThreadStream;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class DumpThread extends Dumper {
    public static void main(String[] args) throws IOException {
        new DumpThread(new File(args[0])).main();
    }

    public DumpThread(File file) throws IOException {
        super(file);
    }

    private void main() throws IOException {
        MethodInfoDictionary dic = new MethodInfoDictionary(rd,null);

        int indent = 0;

        ThreadStream ms = rd.getThreadStream(0);
        System.out.println("Thread "+ms.getName());
        int total = ms.getSize();
        while(ms.hasNext()) {
            RawMethodTrace mc = ms.next();

            if(mc instanceof RawMethodEnter) {
                printIndent(indent);
                MethodInfo method = mc.getMethod(dic);
                System.out.println(method.fullName());
                indent++;
            } else {
                indent--;
            }
        }
    }
}

