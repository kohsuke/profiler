package net.java.dev.profiler.kprofiler.viewer;

import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.java.dev.profiler.kprofiler.viewer.ui_util.JDialogEx;
import net.java.dev.profiler.kprofiler.viewer.model.ViewConfig;
import net.java.dev.profiler.kprofiler.viewer.model.FilterConfig;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class CollapseMethodDialog extends JDialogEx {

    /**
     * The configuration that we are editing.
     */
    private final List<FilterConfig> filters = new ArrayList<FilterConfig>();

    private final ViewConfig config;

    private final JTable table;

    private final JButton add = new JButton("Add...");
    private final JButton remove = new JButton("Remove");
    private final JButton edit = new JButton("Edit...");

    public CollapseMethodDialog(Frame owner, ViewConfig config) throws HeadlessException {
        super(owner, "Collapse Methods");

        this.config = config;
        for( FilterConfig f : config.getFilters() )
            filters.add(f.clone());

        FormLayout layout = new FormLayout(
            "default:grow, 3dlu, pref", // columns
            "fill:default:grow, 7dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        table = createTable();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(table.getBackground());
        builder.add(scrollPane,   cc.xy(1,1));

        ButtonStackBuilder bsb = new ButtonStackBuilder();
        bsb.addGridded(add);
        bsb.addUnrelatedGap();
        bsb.addGridded(remove);
        bsb.addUnrelatedGap();
        bsb.addGridded(edit);
        builder.add(bsb.getPanel(),cc.xy(3,1));

        builder.add(ButtonBarFactory.buildOKCancelBar(okButton,cancelButton),cc.xyw(1,3,3));

        add(builder.getPanel());

        pack();
        setLocationRelativeTo(owner);

        // configure the buttons
        add.setMnemonic('A');
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                add();
            }
        });
        remove.setMnemonic('R');
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // remove all the selected rows
                List<FilterConfig> toRemove = new ArrayList<FilterConfig>();
                for( int idx : table.getSelectedRows() )
                    toRemove.add(filters.get(idx));
                filters.removeAll(toRemove);
                model.fireTableDataChanged();
            }
        });
        edit.setMnemonic('E');
        edit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edit();
            }
        });
        // control enable/disable state of remove/edit buttons.
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateButtons();
            }
        });
        updateButtons();
    }

    private void updateButtons() {
        remove.setEnabled(table.getSelectedRowCount()!=0);
        edit.setEnabled(table.getSelectedRowCount()==1);
    }

    /**
     * Edits the currently selected {@link FilterConfig}.
     */
    private void edit() {
        int index = table.getSelectedRow();
        EditFilterDialog dlg = new EditFilterDialog((Frame) getOwner(), filters.get(index));
        dlg.setVisible(true);
        if(dlg.getReturnCode()==JOptionPane.OK_OPTION)
            model.fireTableRowsUpdated(index,index);
    }

    /**
     * Adds a new {@link FilterConfig}.
     */
    private void add() {
        FilterConfig f = new FilterConfig(true,"","");
        EditFilterDialog dlg = new EditFilterDialog((Frame) getOwner(), f);
        dlg.setVisible(true);
        if(dlg.getReturnCode()==JOptionPane.OK_OPTION) {
            filters.add(f);
            model.fireTableRowsInserted(filters.size()-1,filters.size()-1);
        }
    }

    private JTable createTable() {
        JTable table = new JTable(model);

        table.setPreferredScrollableViewportSize(new Dimension(300,200));

        // fix the width of the first column
        TableColumn col = table.getColumnModel().getColumn(0);
        int w = new JCheckBox().getMinimumSize().width;
        col.setMinWidth(w);
        col.setMaxWidth(w);
        col.setResizable(false);

        return table;
    }

    protected boolean apply() {
        config.setFilters(filters);
        return true;
    }

    /**
     * {@link TableModel} implementation for {@link FilterConfig}.
     */
    private final AbstractTableModel model = new AbstractTableModel() {
        public int getRowCount() {
            return filters.size();
        }

        public int getColumnCount() {
            return 3;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            FilterConfig f = filters.get(rowIndex);
            switch(columnIndex) {
            case 0:
                return f.isEngaged();
            case 1:
                return f.getName();
            case 2:
                return f.getMask().replace('\n',' ');
            }
            throw new IllegalStateException();
        }

        public String getColumnName(int column) {
            switch(column) {
            case 0:
                return " ";
            case 1:
                return "Name";
            case 2:
                return "Mask";
            }
            throw new IllegalStateException();
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // control engage flag through the table
            return columnIndex==0;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            filters.get(rowIndex).setEngaged(((Boolean)aValue).booleanValue());
        }

        public Class<?> getColumnClass(int columnIndex) {
            if(columnIndex==0)  return Boolean.class;
            else                return String.class;
        }
    };
}
