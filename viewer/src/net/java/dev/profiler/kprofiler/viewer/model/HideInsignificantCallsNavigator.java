package net.java.dev.profiler.kprofiler.viewer.model;

import net.java.dev.profiler.kprofiler.MethodInfo;
import net.java.dev.profiler.kprofiler.MethodCall;

import java.util.List;
import java.util.Iterator;

/**
 * Filters out insignificant method calls.
 *
 * <p>
 * if the execution time of the child method is less than N% of that of the parent,
 * the child method is removed.
 *
 * @author Kohsuke Kawaguchi
 */
public class HideInsignificantCallsNavigator<T> implements ModelNavigator<T> {
    private final ModelNavigator<T> core;

    private ViewConfig config;

    public HideInsignificantCallsNavigator(ModelNavigator<T> core, ViewConfig config) {
        this.core = core;
        this.config = config;
    }

    public T getRoot() {
        return core.getRoot();
    }

    public MethodInfo method(T t) {
        return core.method(t);
    }

    public long time(T t) {
        return core.time(t);
    }

    public int callCount(T t) {
        return core.callCount(t);
    }

    public List<? extends T> getChildren(T parent) {
        List<? extends T> children = core.getChildren(parent);

        if(config.isEngaged()) {
            long t = core.time(parent)*config.getThreshold()/100;

            for (Iterator<? extends T> itr = children.iterator(); itr.hasNext();) {
                T child = itr.next();
                if(core.time(child)<t)
                    itr.remove();
            }
        }

        return children;
    }

    public ModelNavigator createSubModel(Iterable<MethodCall> calls) {
        return new HideInsignificantCallsNavigator(core.createSubModel(calls),config);
    }

    public ModelNavigator createSubModel(MethodCall... calls) {
        return new HideInsignificantCallsNavigator(core.createSubModel(calls),config);
    }
}
