package net.java.dev.profiler.kprofiler.raw;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class RawMethodTrace {
    public int methodId;

    public MethodInfo getMethod(MethodInfoDictionary dic) {
        return dic.methods.get(methodId);
    }
}
