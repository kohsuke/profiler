
import net.java.dev.profiler.kprofiler.ProfileDataFactory;
import net.java.dev.profiler.kprofiler.ProfileData;
import net.java.dev.profiler.kprofiler.MethodCall;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Parser {
    public static void main(String[] args) throws IOException {
        ProfileData data = ProfileDataFactory.create(new File(args[0]));

        dump(data.getRoot(),0);
    }

    private static void dump(MethodCall mc, int indent ) {
        for( int i=0; i<indent; i++ )
            System.out.print(' ');

        String name;
        if(mc.method()==null)
            name = "(Root)";
        else
            name = mc.method().fullName();

        System.out.printf("%s (%dcalls / %dus)\n", name, mc.callCount(), mc.time()/1000 );

        for( MethodCall child : mc.children() ) {
            dump(child, indent+1);
        }
    }
}
