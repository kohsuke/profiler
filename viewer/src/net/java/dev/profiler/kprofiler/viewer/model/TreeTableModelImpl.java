package net.java.dev.profiler.kprofiler.viewer.model;

import org.jdesktop.swing.treetable.TreeTableModel;
import net.java.dev.profiler.kprofiler.Format;
import net.java.dev.profiler.kprofiler.MethodInfo;
import net.java.dev.profiler.kprofiler.viewer.ui_util.AbstractTreeTableModelImpl;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@link TreeTableModel} implementation that delegates to
 * {@link ModelNavigator}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class TreeTableModelImpl extends AbstractTreeTableModelImpl {

    private ModelNavigator navigator;

    private final Map<Object,List<Object>> cache = new WeakHashMap<Object, List<Object>>();


    public TreeTableModelImpl(ModelNavigator navigator) {
        this.navigator = navigator;
    }

    public ModelNavigator getNavigator() {
        return navigator;
    }

    public void setNavigator(ModelNavigator navigator) {
        this.navigator = navigator;
    }

    public Object getRoot() {
        return navigator.getRoot();
    }

    public int getColumnCount() {
        return 3;
    }

    public Class getColumnClass(int column) {
        switch(column) {
        case 0:
            return String.class;
        case 1:
            return Number.class;
        case 2:
            return Number.class;
        default:
            throw new IllegalArgumentException();
        }
    }

    public String getColumnName(int column) {
        switch(column) {
        case 0:
            return "Name";
        case 1:
            return "Time";
        case 2:
            return "Invocation Count";
        default:
            throw new IllegalArgumentException();
        }
    }

    public Object getValueAt(Object node, int column) {
        switch(column) {
        case 0:
            MethodInfo m = navigator.method(node);
            if(m!=null) {
                StringBuilder buf = new StringBuilder("<html>");

                String className = m.getClazz().name();
                int idx = className.lastIndexOf('.');
                if(idx>=0)
                    buf.append(className,0,idx).append('.');

                buf.append("<b>").append(className.substring(idx+1)).append('.');

                String name = m.name();
                if(name.charAt(0)=='<')
                    buf.append("&lt;").append(name.substring(1));
                else
                    buf.append(name);
                buf.append("</b>(");

                // parameters
                String[] params = m.parameterTypes(Format.SHORT);
                for( int i=0; i<params.length; i++ ) {
                    if(i!=0)    buf.append(',');
                    buf.append(params[i]);
                }
                buf.append(')');

                return buf.toString();
            } else
                return "(root)";
        case 1:
            return navigator.time(node);
        case 2:
            return navigator.callCount(node);
        default:
            throw new IllegalArgumentException();
        }
    }

    private List getChildren(Object parent) {
        List v = cache.get(parent);
        if(v==null) {
            v = navigator.getChildren(parent);
            cache.put(parent,v);
        }
        return v;
    }

    public Object getChild(Object parent, int index) {
        return getChildren(parent).get(index);
    }

    public int getChildCount(Object parent) {
        return getChildren(parent).size();
    }

    public int getIndexOfChild(Object parent, Object child) {
        return getChildren(parent).indexOf(child);
    }

    public boolean isLeaf(Object parent) {
        return getChildren(parent).isEmpty();
    }

    public void fireTreeChange() {
        cache.clear();
        super.fireTreeChange();
    }

}
