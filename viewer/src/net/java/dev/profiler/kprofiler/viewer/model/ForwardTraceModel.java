package net.java.dev.profiler.kprofiler.viewer.model;

import net.java.dev.profiler.kprofiler.MethodCall;
import net.java.dev.profiler.kprofiler.MethodInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author Kohsuke Kawaguchi
 */
public class ForwardTraceModel implements ModelNavigator<Object/*MethodCall or MethodCall[]*/> {
    private final Object root;

    private MethodCallFilter filter;

    public ForwardTraceModel(MethodCall... root) {
        if(root.length==1)
            this.root = root[0];
        else
            this.root = root;
    }

    public ForwardTraceModel(MethodCall[] roots, MethodCallFilter filter) {
        this(roots);
        setFilter(filter);
    }

    public ForwardTraceModel(Iterable<MethodCall> root) {
        this(toArray(root));
    }

    private static MethodCall[] toArray(Iterable<MethodCall> root) {
        List<MethodCall> r = new ArrayList<MethodCall>();
        for( MethodCall mc : root )
            r.add(mc);
        return r.toArray(new MethodCall[r.size()]);
    }

    public MethodCallFilter getFilter() {
        return filter;
    }

    public void setFilter(MethodCallFilter filter) {
        this.filter = filter;
    }

    public Object getRoot() {
        return root;
    }

    public MethodInfo method(Object t) {
        if(t instanceof MethodCall[])
            return ((MethodCall[])t)[0].method();
        else
            return ((MethodCall)t).method();
    }

    public long time(Object t) {
        if(t instanceof MethodCall[]) {
            long r=0;
            for( MethodCall item : (MethodCall[])t )
                r += item.time();
            return r;
        } else
            return ((MethodCall)t).time();
    }

    public int callCount(Object t) {
        if(t instanceof MethodCall[]) {
            int r=0;
            for( MethodCall item : (MethodCall[])t )
                r += item.callCount();
            return r;
        } else
            return ((MethodCall)t).callCount();
    }


    public List<?> getChildren(Object parent) {
        Stack<MethodCall> workQueue = new Stack<MethodCall>();

        if (parent instanceof MethodCall[]) {
            for( MethodCall t : (MethodCall[]) parent )
                workQueue.push(t);
        } else {
            workQueue.push((MethodCall)parent);
        }

        Map<MethodInfo,Object/*List of just T*/> buf = new HashMap<MethodInfo,Object>();

        while(!workQueue.isEmpty()) {
            MethodCall head = workQueue.pop();
            for( MethodCall child : head.children() ) {
                if(filter!=null && filter.shallBeCollapsed(head,child)) {
                    workQueue.push(child);
                    continue;
                } else {
                    MethodInfo key = child.method();
                    Object o = buf.get(key);
                    if(o==null) {
                        buf.put(key,child);
                    } else {
                        if(o instanceof MethodCall) {
                            List<MethodCall> list = new ArrayList<MethodCall>();
                            list.add(child);
                            list.add((MethodCall)o);
                            buf.put(key,list);
                        } else {
                            ((List<MethodCall>)o).add(child);
                        }
                    }
                }
            }
        }

        // then create children
        List<Object> r = new ArrayList<Object>();
        for (Map.Entry<MethodInfo, Object> e : buf.entrySet()) {
            if(e.getValue() instanceof List) {
                List<MethodCall> list = (List<MethodCall>) e.getValue();
                r.add(list.toArray(new MethodCall[list.size()]));
            } else {
                r.add( e.getValue() );
            }
        }

        return r;
    }

    /**
     * Creates a new {@link ModelNavigator} that visits the subset of this model
     * rooted at the given calls.
     */
    public ModelNavigator createSubModel(Iterable<MethodCall> calls) {
        List<MethodCall> r = new ArrayList<MethodCall>();
        for( MethodCall mc : calls ) {
            if(isDescendant(mc))
                r.add(mc);
        }
        return new ForwardTraceModel(r.toArray(new MethodCall[r.size()]),filter);
    }
    public ModelNavigator createSubModel(MethodCall... calls) {
        List<MethodCall> r = new ArrayList<MethodCall>();
        for( MethodCall mc : calls ) {
            if(isDescendant(mc))
                r.add(mc);
        }
        return new ForwardTraceModel(r.toArray(new MethodCall[r.size()]),filter);
    }

    private boolean isDescendant(MethodCall mc) {
        if(root.equals(mc))
            return true;
        if(root instanceof MethodCall[]) {
            for( MethodCall r : (MethodCall[])root )
                if(r.equals(mc))
                    return true;
        }
        MethodCall p = mc.getParent();
        if(p==null)
            return false;

        return isDescendant(p);
    }
}
