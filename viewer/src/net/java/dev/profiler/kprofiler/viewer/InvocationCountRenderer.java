package net.java.dev.profiler.kprofiler.viewer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * {@link TableCellRenderer} to draw the invocation count.
 *
 * @author Kohsuke Kawaguchi
 */
final class InvocationCountRenderer extends DefaultTableCellRenderer {
    public InvocationCountRenderer() {
        setHorizontalAlignment(SwingConstants.RIGHT);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // don't draw the focus border to give users the impression
        // that there's really just one column
        return super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
    }
}
