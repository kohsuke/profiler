package net.java.dev.profiler.kprofiler.viewer;

import org.jdesktop.swing.table.ColumnHeaderRenderer;
import org.jdesktop.swing.icon.SortArrowIcon;
import org.jdesktop.swing.JXTable;
import org.jdesktop.swing.decorator.Sorter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class TreeColumnHeaderRenderer extends JPanel implements TableCellRenderer {

    private static TableCellRenderer	sharedInstance = null;
    private static Icon					defaultDownIcon = new SortArrowIcon(false);
    private static Icon					defaultUpIcon = new SortArrowIcon(true);
    private static Border               defaultMarginBorder = new EmptyBorder(0,2,0,2);

    private Icon    downIcon = defaultDownIcon;
    private Icon    upIcon = defaultUpIcon;
    private JLabel	label = new JLabel("", JLabel.CENTER);
    private JLabel	arrow = new JLabel((Icon) null, JLabel.CENTER);
    private boolean antiAliasedText = false;

    private boolean backgroundSet = false;
    private boolean foregroundSet = false;
    private boolean fontSet = false;

    public static TableCellRenderer getSharedInstance() {
         if (sharedInstance == null) {
             sharedInstance = new TreeColumnHeaderRenderer();
         }
         return sharedInstance;
     }

    private TreeColumnHeaderRenderer() {
        setLayout(new BorderLayout());
        // initialize default properties
        Font boldFont = label.getFont().deriveFont(Font.BOLD);
        label.setFont(boldFont);
        label.setOpaque(false);
        add(label);
        add(arrow, BorderLayout.EAST);

        arrow.setBorder(defaultMarginBorder);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        label.setText(value == null ? "" : value.toString());
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            // inherit properties from header only if they were not set
            // on a per-column-header basis
            if (!foregroundSet) {
                setForeground(header.getForeground());
                foregroundSet = false;
            }
            if (!backgroundSet) {
                setBackground(header.getBackground());
                backgroundSet = false;
            }
            if (!fontSet) {
                setFont(header.getFont());
                fontSet = false;
            }
        }

        arrow.setIcon(null);
//        arrow.setIcon(downIcon);

        /**@todo aim: setting this every time seems inefficient? */
        setBorder(new CompoundBorder(
            UIManager.getBorder("TableHeader.cellBorder"),
            defaultMarginBorder));
        return this;
    }

    public void setAntiAliasedText(boolean antiAlias) {
        this.antiAliasedText = antiAlias;
    }

    public boolean getAntiAliasedText() {
        return antiAliasedText;
    }

    public void setBackground(Color background) {
        backgroundSet = true;
        super.setBackground(background);
    }

    public void setForeground(Color foreground) {
        foregroundSet = true;
        super.setForeground(foreground);
        if (label != null) {
            label.setForeground(foreground);
        }
    }

    public void setFont(Font font) {
        fontSet = true;
        super.setFont(font);
        if (label != null) {
            label.setFont(font);
        }
    }

    public void setDownIcon(Icon icon) {
        this.downIcon = icon;
    }

    public Icon getDownIcon() {
        return downIcon;
    }

    public void setUpIcon(Icon icon) {
        this.upIcon = icon;
    }

    public Icon getUpIcon() {
        return upIcon;
    }

    public void setHorizontalAlignment(int alignment) {
        label.setHorizontalAlignment(alignment);
    }

    public int getHorizontalAlignment() {
        return label.getHorizontalAlignment();
    }

    public void setHorizontalTextPosition(int textPosition) {
        label.setHorizontalTextPosition(textPosition);
    }

    public int getHorizontalTextPosition() {
        return label.getHorizontalTextPosition();
    }

    public void setIcon(Icon icon) {
        label.setIcon(icon);
    }

    public Icon getIcon() {
        return label.getIcon();
    }

    public void setIconTextGap(int iconTextGap) {
        label.setIconTextGap(iconTextGap);
    }

    public int getIconTextGap() {
        return label.getIconTextGap();
    }

    public void setVerticalAlignment(int alignment) {
        label.setVerticalAlignment(alignment);
    }

    public int getVerticalAlignment() {
        return label.getVerticalAlignment();
    }

    public void setVerticalTextPosition(int textPosition) {
        label.setVerticalTextPosition(textPosition);
    }

    public int getVerticalTextPosition() {
        return label.getVerticalTextPosition();
    }

    public void paint(Graphics g) {
        if (antiAliasedText) {
            Graphics2D g2 = (Graphics2D) g;
            Object save = g2.getRenderingHint(RenderingHints.
                                              KEY_TEXT_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            super.paint(g2);

            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, save);
        } else {
            super.paint(g);
        }
    }
}
