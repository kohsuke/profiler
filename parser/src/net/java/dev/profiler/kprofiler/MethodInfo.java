package net.java.dev.profiler.kprofiler;

/**
 * Information about a profiled method
 *
 * @author Kohsuke Kawaguchi
 */
public interface MethodInfo {
    /**
     * The class to which this method belongs.
     */
    ClassInfo getClazz();

    /**
     * Just the method name (such as "toString")
     */
    String name();

    /**
     * Method signature.
     * <p>
     * For example, "()V" or "(IDLjava/lang/Thread;)Ljava/lang/Object;"
     * <p>
     * See <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#7035">
     * the section 4.3.3 of the JVM spec</a> for the details of the format.
     */
    String signature();

    /**
     * Gets the name of the method return type.
     */
    String returnType(Format format);

    /**
     * Gets the parameter types.
     *
     * @return
     *      can be an empty array but never be null.
     */
    String[] parameterTypes(Format format);

    /**
     * Creates a new cursor that points to the first {@link MethodCall} that
     * calls this method.
     */
    MethodCall firstCall();

    /**
     * Iterates all the {@link MethodCall}s that invokes this method.
     */
    Iterable<MethodCall> calls();

    /**
     * Gets the fully qualified method name.
     */
    String fullName();
}
