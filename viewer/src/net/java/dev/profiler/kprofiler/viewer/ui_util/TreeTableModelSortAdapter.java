package net.java.dev.profiler.kprofiler.viewer.ui_util;

import org.jdesktop.swing.JXTreeTable;
import org.jdesktop.swing.JXTree;
import org.jdesktop.swing.treetable.AbstractTreeTableModel;
import org.jdesktop.swing.treetable.TreeTableModel;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.Comparator;

/**
 * {@link TreeTableModel} that wraps another {@link TreeTableModel}
 * and sort its children.
 *
 * @author Kohsuke Kawaguchi
 */
public class TreeTableModelSortAdapter extends AbstractTreeTableModel implements TreeModelListener {
    private final TreeTableModel core;
    private Comparator comparator;

    /**
     * Cache. The parent of {@link #children}.
     * <p>
     * To improve the performance, we keep the sorted children list for the parent
     * object that was asked the last time. Therefore, for example, if {@link #getChild(Object, int)}
     * method is invoked twice with the same parent object, the 2nd invocation
     * will hit this cache.
     * <p>
     * This should improve the performance if {@link JXTreeTable} invokes various
     * methods with the same parent.
     * <p>
     * Note that a change in the underlying model
     */
    private Object lastParent;
    /**
     * Sorted list of children.
     */
    private Object[] children;

    public TreeTableModelSortAdapter(TreeTableModel core, Comparator comparator) {
        this.core = core;
        core.addTreeModelListener(this);
        setComparator(comparator);
    }

    public Comparator getComparator() {
        return comparator;
    }

    /**
     * Sets the {@link Comparator} used to sort siblings.
     */
    public void setComparator(Comparator comparator) {
        if(comparator==null)
            throw new IllegalArgumentException();
        if(comparator==this.comparator)
            return; // noop
        clearCache();
        this.comparator = comparator;
        fireTreeStructureChanged(this,new Object[]{getRoot()},null,null);
    }


    /**
     * This isn't defined on the {@link TreeTableModel} interface,
     * but nevertheless this is used by {@link JXTree} to convert
     * the model object to the value that gets displayed.
     */
    public Object convertValueToText(Object o) {
        return getValueAt(o,0);
    }


    public Object getChild(Object parent, int index) {
        return getChildren(parent)[index];
    }

    public int getIndexOfChild(Object parent, Object child) {
        Object[] children = getChildren(parent);
        for( int i=0; i<children.length; i++ )
            if(children[i].equals(child))
                return i;
        return -1;
    }

    private Object[] getChildren(Object parent) {
        if(parent==lastParent)
            return children;    // cache hit

        lastParent = parent;
        children = new Object[core.getChildCount(parent)];
        for( int i=0; i<children.length; i++ )
            children[i] = core.getChild(parent,i);
        Arrays.sort(children,comparator);
        return children;
    }


    public void addTreeModelListener(TreeModelListener l) {
        core.addTreeModelListener(l);
        super.addTreeModelListener(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        core.removeTreeModelListener(l);
        super.removeTreeModelListener(l);
    }


//
// cache control methods
//

    public void treeNodesChanged(TreeModelEvent e) {
        selectiveCachePurge(e);
    }

    public void treeNodesInserted(TreeModelEvent e) {
        selectiveCachePurge(e);
    }

    public void treeNodesRemoved(TreeModelEvent e) {
        selectiveCachePurge(e);
    }

    public void treeStructureChanged(TreeModelEvent e) {
        clearCache();
    }

    /**
     * If the underlying model has changed, erase the cache
     * so that we won't return incorrect data.
     */
    private void selectiveCachePurge(TreeModelEvent e) {
        Object[] path = e.getPath();
        if(lastParent==path[path.length-1])
            clearCache();
    }

    /**
     * Erases the cache.
     */
    private void clearCache() {
        lastParent = null;
    }

//
// simple delegation to TreeTableModel
//
    public Class getColumnClass(int column) {
        return core.getColumnClass(column);
    }

    public int getColumnCount() {
        return core.getColumnCount();
    }

    public String getColumnName(int column) {
        return core.getColumnName(column);
    }

    public Object getValueAt(Object node, int column) {
        return core.getValueAt(node, column);
    }

    public boolean isCellEditable(Object node, int column) {
        return core.isCellEditable(node, column);
    }

    public void setValueAt(Object value, Object node, int column) {
        core.setValueAt(value, node, column);
    }

    public Object getRoot() {
        return core.getRoot();
    }

    public int getChildCount(Object parent) {
        return core.getChildCount(parent);
    }

    public boolean isLeaf(Object node) {
        return core.isLeaf(node);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        core.valueForPathChanged(path, newValue);
    }
}
