
import net.java.dev.profiler.kprofiler.raw.RawDataFile;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Dumper {
    protected final RawDataFile rd;

    public Dumper(File file) throws IOException {
        rd = new RawDataFile(file);
    }

    protected final void printIndent(int indent) {
        for(int i=0; i<indent; i++ )
            System.out.print(' ');
    }
}
