package net.java.dev.profiler.kprofiler.viewer.model;

import net.java.dev.profiler.kprofiler.MethodCall;

/**
 * @author Kohsuke Kawaguchi
 */
public interface MethodCallFilter {
    boolean shallBeCollapsed(MethodCall parent, MethodCall child);
}
