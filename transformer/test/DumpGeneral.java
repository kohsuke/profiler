
import net.java.dev.profiler.kprofiler.raw.NamedRecord;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
public class DumpGeneral extends Dumper {
    public static void main(String[] args) throws IOException {
        new DumpGeneral(new File(args[0])).main();
    }

    public DumpGeneral(File file) throws IOException {
        super(file);
    }

    private void main() throws IOException {
        Iterator<NamedRecord> gs = rd.getGeneralStream();

        while(gs.hasNext()) {
            NamedRecord r = gs.next();
            System.out.println(r.name+'='+r.data);
        }
    }
}
