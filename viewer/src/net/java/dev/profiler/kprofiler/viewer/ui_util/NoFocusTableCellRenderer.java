package net.java.dev.profiler.kprofiler.viewer.ui_util;

import javax.swing.table.TableCellRenderer;
import javax.swing.*;
import java.awt.*;

/**
 * Decorates another {@link TableCellRenderer} by hiding the focus rect.
 *
 * @author Kohsuke Kawaguchi
 */
public class NoFocusTableCellRenderer implements TableCellRenderer {
    private final TableCellRenderer renderer;

    public NoFocusTableCellRenderer(TableCellRenderer renderer) {
        this.renderer = renderer;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return renderer.getTableCellRendererComponent(table, value, isSelected, false, row, column);
    }
}
