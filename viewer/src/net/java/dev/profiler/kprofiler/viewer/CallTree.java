package net.java.dev.profiler.kprofiler.viewer;

import org.jdesktop.swing.JXTreeTable;
import org.jdesktop.swing.treetable.TreeTableModel;
import net.java.dev.profiler.kprofiler.MethodCall;
import net.java.dev.profiler.kprofiler.MethodInfo;
import net.java.dev.profiler.kprofiler.viewer.model.HideInsignificantCallsNavigator;
import net.java.dev.profiler.kprofiler.viewer.model.ModelNavigator;
import net.java.dev.profiler.kprofiler.viewer.model.TreeTableModelImpl;
import net.java.dev.profiler.kprofiler.viewer.model.ViewConfig;
import net.java.dev.profiler.kprofiler.viewer.ui_util.NoFocusTableCellRenderer;
import net.java.dev.profiler.kprofiler.viewer.ui_util.TreeTableModelSortAdapter;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@link JXTreeTable} that shows the method call tree.
 *
 * @author Kohsuke Kawaguchi
 */
public class CallTree extends JXTreeTable {
    /**
     * Used to draw the time column.
     */
    private final TimeRenderer timeRenderer = new TimeRenderer();

    /**
     * Popup menu.
     */
    private final JPopupMenu popup = new JPopupMenu();

    /**
     * Current model that we are displaying.
     */
    private ModelNavigator model;

    private HideInsignificantCallsNavigator hicNav;

    private final MainFrame mainWindow;

    private TreeTableModelImpl treeModel;

    public CallTree(MainFrame parent) {
        this.mainWindow = parent;

        // don't change the foreground color of the row even if it's selected,
        // because some of the items are rendered in different colors.
        setSelectionBackground(new Color(200,200,255));
        setSelectionForeground(getForeground());

        initPopupMenu();

        // on Windows, a right mouse down should change the selection
        // (so that the pop-up menu can be displayed for the row that you clicked.
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                getSelectionModel().setLeadSelectionIndex(rowAtPoint(e.getPoint()));
            }
        });


        final ViewConfig.ChangeListener viewConfigListener = new ViewConfig.ChangeListener() {
            public void onChanged() {
                // notify the listeners of the tree model that the view has changed
                treeModel.fireTreeChange();
            }
        };
        mainWindow.viewConfig.addChangeListener(viewConfigListener);
        addComponentListener(new ComponentAdapter() {
            public void componentHidden(ComponentEvent e) {
                mainWindow.viewConfig.removeChangeListener(viewConfigListener);
            }
        });
    }

    private void initPopupMenu() {
        JMenu drillDown = new JMenu("View this method as root");
        popup.add(drillDown);

        JMenuItem viewMethod = new JMenuItem("Just this invocation");
        viewMethod.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object sel = getTreeSelectionModel().getSelectionPath().getLastPathComponent();
                if(sel instanceof MethodCall) {
                    MethodCall mc = (MethodCall) sel;
                    mainWindow.open(mc.method().fullName(),model.createSubModel(mc));
                } else {
                    MethodCall[] mc = (MethodCall[]) sel;
                    mainWindow.open(mc[0].method().fullName(),model.createSubModel(mc));
                }
            }
        });
        drillDown.add(viewMethod);

        JMenuItem viewAllInvocations = new JMenuItem("Merge all invocations of this method");
        viewAllInvocations.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object sel = getTreeSelectionModel().getSelectionPath().getLastPathComponent();
                if(sel instanceof MethodCall) {
                    MethodCall mc = (MethodCall) sel;
                    mainWindow.open(mc.method().fullName(),model.createSubModel(mc.method().calls()));
                } else {
                    MethodCall[] mc = (MethodCall[]) sel;

                    List<MethodInfo> methods = new ArrayList<MethodInfo>();
                    for( MethodCall m : mc )
                        methods.add(m.method());

                    List<MethodCall> roots = new ArrayList<MethodCall>();
                    for( MethodInfo m : methods )
                        for( MethodCall call : m.calls() )
                            roots.add(call);

                    mainWindow.open(mc[0].method().fullName(),model.createSubModel(roots));
                }
            }
        });
        drillDown.add(viewAllInvocations);

        setComponentPopupMenu(popup);
    }

    /**
     * @see TimeRenderer#setDivideFactor(int)
     */
    public void setDivideFactor(int factor) {
        timeRenderer.setDivideFactor(factor);
    }

    // this is necessary for us to have nodes that don't implement TreeNode.
    public boolean isHierarchical(int column) {
        return column==0;
    }

    /**
     * Changes the data to be displayed.
     * <p>
     * This also sets the root node to be the root method call of the data.
     */
    public void setModel(ModelNavigator _model) {
        this.model = _model;
        this.hicNav = new HideInsignificantCallsNavigator(model,mainWindow.viewConfig);
        this.treeModel = new TreeTableModelImpl(hicNav);
        setTreeTableModel(
            new TreeTableModelSortAdapter(treeModel,
                    new Comparator() {
                        public int compare(Object o1, Object o2) {
                            long t1 = model.time(o1);
                            long t2 = model.time(o2);
                            if(t1<t2)   return 1;
                            if(t1>t2)   return -1;
                            else        return 0;
                        }
                    }));
        setCellRenderer(new TreeCellRendererImpl());
        getTableHeader().setDefaultRenderer(TreeColumnHeaderRenderer.getSharedInstance());
        UIManager.put("Table.focusCellHighlightBorder",null);


        TableColumn column;

        column = getColumnModel().getColumn(0);
        // intercept the cell renderer for the first column.
        // because JXTreeTable hides it, this seems to be the easiest way
        // to do it. column.setCellRenderer doesn't work.
        //
        // here, I'm just hiding the focus rect.
        TableCellRenderer dr = getDefaultRenderer(TreeTableModel.class);
        ((JTree)dr).addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded(TreeExpansionEvent event) {
                // if a node is opened and it only has one child, open that too
                List children = hicNav.getChildren(event.getPath().getLastPathComponent());
                if(children.size()==1)
                    expandPath(event.getPath().pathByAddingChild(children.get(0)));
            }
            public void treeCollapsed(TreeExpansionEvent event) {
            }
        });
        column.setCellRenderer(new NoFocusTableCellRenderer(dr));
        column.setPreferredWidth(9999);

        column = getColumnModel().getColumn(1);
        column.setCellRenderer(timeRenderer);
        column.setMinWidth(timeRenderer.getPreferredSize().width);

        column = getColumnModel().getColumn(2);
        column.setCellRenderer(new InvocationCountRenderer());
        column.setPreferredWidth(50);
        column.setMinWidth(50);

        setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        timeRenderer.setPercentageBase(model.time(model.getRoot()));

        setRootVisible(true);
        setShowsRootHandles(true);
        setRowSelectionAllowed(true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
//
//        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
}
