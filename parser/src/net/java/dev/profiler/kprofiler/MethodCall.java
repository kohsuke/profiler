package net.java.dev.profiler.kprofiler;

/**
 * Data record about method invocations.
 *
 * <p>
 * Unlike a typical object model, where all the components
 * are statically created and navigation methods only return
 * the pre-created objects, {@link MethodCall}s are created
 * on-the-fly by the navigation methods.
 *
 * <p>
 * Therefore, you can get two different {@link MethodCall}s
 * (that are {@link #equals(Object) equal} that
 * describe the same information.
 *
 * @author Kohsuke Kawaguchi
 */
public interface MethodCall {

    //
    // MethodCalls form a tree structure called "invocation tree"
    //

    MethodCall getFirstChild();

    MethodCall getNextSibling();

    Iterable<MethodCall> children();


    /**
     * Gets the parent method invocation.
     */
    MethodCall getParent();

    /**
     * Gets the next {@link MethodCall} that calls the same method
     * (but called from a different parent)
     */
    MethodCall getNextByMethod();


    /**
     * Number of times this method is called by its parent.
     */
    int callCount();

    /**
     * Total time spent in this method (and its descendants) in nano-seconds.
     */
    long time();

    /**
     * Obtain the information about the method that was called.
     *
     * @return
     *      always non-null.
     */
    MethodInfo method();
}
