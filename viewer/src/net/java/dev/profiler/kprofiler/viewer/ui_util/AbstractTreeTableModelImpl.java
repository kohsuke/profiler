package net.java.dev.profiler.kprofiler.viewer.ui_util;

import org.jdesktop.swing.JXTree;
import org.jdesktop.swing.treetable.TreeTableModel;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

/**
 * Default partial implementation of {@link TreeTableModel}.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractTreeTableModelImpl implements TreeTableModel {
    private TypedEventListeners<TreeModelListener> listenerList
            = new TypedEventListeners<TreeModelListener>(TreeModelListener.class);

    public boolean isCellEditable(Object node, int column) {
        return false;
    }

    public void setValueAt(Object value, Object node, int column) {
        throw new UnsupportedOperationException();
    }

    /**
     * This isn't defined on the {@link TreeTableModel} interface,
     * but nevertheless this is used by {@link JXTree} to convert
     * the model object to the value that gets displayed.
     */
    public Object convertValueToText(Object o) {
        return getValueAt(o,0);
    }

    public void addTreeModelListener(TreeModelListener l) {
        listenerList.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listenerList.remove(l);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        // no cells are editable
        throw new UnsupportedOperationException();
    }

    /**
     * Fires an event telling that everything has changed.
     */
    protected void fireTreeChange() {
        TreeModelEvent e = new TreeModelEvent(this,new TreePath(getRoot()));
        listenerList.getSink().treeStructureChanged(e);
    }
}
