package net.java.dev.profiler.kprofiler.transformer;

import net.java.dev.profiler.kprofiler.raw.MethodCall;
import net.java.dev.profiler.kprofiler.raw.MethodInfoDictionary;
import net.java.dev.profiler.kprofiler.raw.RawDataFile;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) throws IOException {
        File source = new File(args[0]);
        File dest;
        if(args.length==1) {
            dest = Transformer.getDefaultOutput(source);
        } else
            dest = new File(args[1]);

        System.out.println("Writing to "+dest);
        new Main(source).main(dest);
    }

    private final RawDataFile rd;

    public Main(File file) throws IOException {
        rd = new RawDataFile(file);
    }

    private void main(File out) throws IOException {
        MethodInfoDictionary dic = new MethodInfoDictionary(rd,null);

        Transformer transformer = new Transformer(dic);
        transformer.transform(rd.getThreadStream(0));
//        dump(transformer.getRoot(),dic,0);
        transformer.write(out);
    }


    private void dump(MethodCall mc, MethodInfoDictionary dic, int indent) {
        printIndent(indent);

        String name;
        if(mc.id==0)
            name = "(Root)";
        else
            name = mc.getMethod(dic).fullName();
        System.out.printf("%s (%dcalls / %dus)\n", name, mc.callCount, mc.microTime(rd) );

        boolean misc = false;

        for( MethodCall child : mc ) {
            if(child.time>0)
                dump(child, dic, indent+1);
            else {
                if(!misc) {
                    printIndent(indent+1);
                    System.out.println("...");
                    misc = true;
                }
            }
        }
    }

    private void printIndent(int indent) {
        for(int i=0; i<indent; i++ )
            System.out.print(' ');
    }
}
