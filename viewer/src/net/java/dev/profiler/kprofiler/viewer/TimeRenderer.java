package net.java.dev.profiler.kprofiler.viewer;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Used to render the method execution time column.
 *
 * @author Kohsuke Kawaguchi
 */
final class TimeRenderer extends JPanel implements TableCellRenderer {
    private final JLabel left;
    private final JLabel right;

    /**
     * Time displayed in the table is divided by this number before display.
     *
     * set 1000 to display us, set 1000000 to display ms.
     */
    private int divideFactor=1000;

    /**
     * Size of the {@link #right}.
     */
    private Dimension rightSize;

    /**
     * The percentage is against this value.
     */
    private long percentageBase;

    public TimeRenderer() {
        setLayout(new BorderLayout());
        add(left=new JLabel());
        add(right=new JLabel(),BorderLayout.EAST);

        left.setHorizontalAlignment(SwingConstants.RIGHT);
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        right.setForeground(Color.GRAY);

        left.setText("10,000,000");
        right.setText("<html>100<small>%</small>");
        Dimension sz = right.getPreferredSize();
        rightSize = new Dimension( sz.width+16, sz.height );

        setPreferredSize( new Dimension( left.getPreferredSize().width+rightSize.width, sz.height ) );
    }

    public void setDivideFactor(int divideFactor) {
        this.divideFactor = divideFactor;
    }

    public void setPercentageBase(long percentageBase) {
        this.percentageBase = percentageBase;
    }

    private static final NumberFormat formatter = NumberFormat.getInstance(Locale.ENGLISH);

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        long l = ((Number) value).longValue();

        left.setText(formatter.format(l/divideFactor));
        if(percentageBase!=0)
            right.setText("<html>"+(l*100/percentageBase)+"<small>%</small>");
        else
            right.setText("");
        right.setPreferredSize(rightSize);

        // the foreground color of the right is always gray.
        if (isSelected) {
            left.setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            left.setForeground(table.getForeground());
            super.setBackground(table.getBackground());
        }

        left.setFont(table.getFont());
        right.setFont(table.getFont());

        return this;
    }
}
