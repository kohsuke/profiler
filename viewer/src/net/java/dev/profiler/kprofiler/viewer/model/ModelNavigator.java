package net.java.dev.profiler.kprofiler.viewer.model;

import net.java.dev.profiler.kprofiler.MethodInfo;
import net.java.dev.profiler.kprofiler.MethodCall;

import java.util.List;

/**
 * Provides a view on {@link model data}.
 *
 * @param <T>
 *      the type of the model object.
 *      TODO: is this parameterization still necessary?
 *
 * @author Kohsuke Kawaguchi
 */
public interface ModelNavigator<T> {
    T getRoot();
    MethodInfo method(T t);
    long time(T t);
    int callCount(T t);
    List<? extends T> getChildren(T parent);

    ModelNavigator createSubModel(Iterable<MethodCall> calls);
    ModelNavigator createSubModel(MethodCall... calls);
}