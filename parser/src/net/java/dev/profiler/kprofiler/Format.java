package net.java.dev.profiler.kprofiler;

/**
 * Mode of the format to print the type name.
 *
 * @author Kohsuke Kawaguchi
 */
public enum Format {
    /**
     * Short name. Uses just a class name without a package name.
     * <p>
     * In this mode, you get type names like "int", "String[]", "BigInteger".
     */
    SHORT,
    /**
     * Fully-qualified name. Uses a fully qualified clsas name.
     * <p>
     * In this mode, you get type names like "int", "java.lang.String[]", "java.math.BigInteger".
     */
    FULLY_QUALIFIED;


    public static class Result {
        public final String typeName;
        public final String restOfSig;

        public Result(String typeName, String restOfSig) {
            this.typeName = typeName;
            this.restOfSig = restOfSig;
        }
    }
    /**
     * Formats the type name in the VM format to a normal Java type name.
     * <p>
     * Such as "V" -> "void", "Ljava/lang/Object;" -> "java.lang.Object"/"Object"
     */
    public Result format(String sig) {
        char ch = sig.charAt(0);
        switch(ch) {
        case 'V':   return new Result("void",sig.substring(1));
        case 'B':   return new Result("byte",sig.substring(1));
        case 'C':   return new Result("char",sig.substring(1));
        case 'D':   return new Result("double",sig.substring(1));
        case 'F':   return new Result("float",sig.substring(1));
        case 'I':   return new Result("int",sig.substring(1));
        case 'J':   return new Result("long",sig.substring(1));
        case 'S':   return new Result("short",sig.substring(1));
        case 'Z':   return new Result("boolean",sig.substring(1));
        case '[':
            Result r = format(sig.substring(1));
            return new Result(r.typeName+"[]",r.restOfSig);
        case 'L':
            int idx = sig.indexOf(';');
            String typeName = sig.substring(1, idx).replace('/', '.');
            if(this==SHORT) {
                typeName = typeName.substring(typeName.lastIndexOf('.')+1);
            }
            return new Result(typeName,sig.substring(idx+1));
        default:
            throw new IllegalArgumentException(sig);
        }
    }
}
