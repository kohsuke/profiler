package net.java.dev.profiler.kprofiler.viewer;

import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.*;
import java.awt.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class TreeCellRendererImpl extends DefaultTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, false);
        setIcon(null);
        return component;
    }
}
