package net.java.dev.profiler.kprofiler.viewer;

import com.jgoodies.plaf.HeaderStyle;
import com.jgoodies.plaf.Options;
import net.java.dev.profiler.kprofiler.ProfileData;
import net.java.dev.profiler.kprofiler.ProfileDataFactory;
import net.java.dev.profiler.kprofiler.raw.MethodInfoDictionary;
import net.java.dev.profiler.kprofiler.raw.RawDataFile;
import net.java.dev.profiler.kprofiler.transformer.Transformer;
import net.java.dev.profiler.kprofiler.viewer.model.ModelNavigator;
import net.java.dev.profiler.kprofiler.viewer.model.ForwardTraceModel;
import net.java.dev.profiler.kprofiler.viewer.model.ViewConfig;
import net.java.dev.profiler.kprofiler.viewer.ui_util.FileDragAndDropDecorator;
import net.java.dev.profiler.kprofiler.viewer.ui_util.JMenuEx;
import net.java.dev.profiler.kprofiler.viewer.ui_util.MRUMenuFactory;
import net.java.dev.profiler.kprofiler.viewer.ui_util.RadioButtonMenuFactory;
import net.java.dev.profiler.kprofiler.viewer.ui_util.WindowPlacementPersistenceDecorator;
import org.jdesktop.swing.JXStatusBar;
import org.jdesktop.swing.JXFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main window of the viewer.
 *
 * @author Kohsuke Kawaguchi
 */
public class MainFrame extends JXFrame {

    private final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);

    private MRUMenuFactory mruMenuFactory;

    private final JXStatusBar statusBar = new JXStatusBar();

    /**
     * Shared by {@link CallTree}s as the filtering configuration.
     */
    final ViewConfig viewConfig = ViewConfig.create();

    /**
     * True to show FQCN.
     * TODO: move to {@link ViewConfig}.
     */
    private boolean useFullName = false;

    /** Creates new form NewJFrame */
    public MainFrame() {
        super("KProfiler");
        setIconImage(Toolkit.getDefaultToolkit().createImage(MainFrame.class.getResource("icons/trace_co.gif")));
        WindowPlacementPersistenceDecorator.decorate(this);
        FileDragAndDropDecorator.decorate(this,new FileDragAndDropDecorator.Listener() {
            public boolean onDrop(File file) {
                return open(file);
            }
        });
        initComponents();
    }

    private void initComponents() {
        initMenu();

        getContentPane().setLayout(new BorderLayout());

//        tabbedPane.putClientProperty(Options.EMBEDDED_TABS_KEY, Boolean.TRUE);

        getContentPane().add(tabbedPane);
        tabbedPane.setFocusable(false); // otherwise we'll see ugly orange box around text
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int sel = tabbedPane.getSelectedIndex();

                for(int i = tabbedPane.getTabCount()-1; i>=0; i--) {
                    String title = tabbedPane.getTitleAt(i);
                    if(title.startsWith("<html>"))
                        title = title.substring(6);
                    if(title.startsWith("<b>"))
                        title = title.substring(3);
                    if(i==sel)
                        title = "<html><b>"+title;
                    tabbedPane.setTitleAt(i,title);
                }
            }
        });
        // don't show borders around the content component.
        tabbedPane.putClientProperty("jgoodies.noContentBorder", Boolean.TRUE);

        getContentPane().add(statusBar,BorderLayout.SOUTH);
        statusBar.setBorder(new EmptyBorder(0,5,0,5));
    }

    private void initMenu() {
        JMenuBar bar = new JMenuBar();
        bar.add(createFileMenu());
        bar.add(createViewMenu());

        setJMenuBar(bar);
        bar.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.SINGLE);
    }

    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem open = new JMenuItem("Open...");
        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // use AWT file chooser so that we can use the native dialog
                FileDialog fileDialog = new FileDialog(MainFrame.this, "File Open",FileDialog.LOAD);
                fileDialog.setVisible(true);
                if(fileDialog.getFile()!=null) {
                    open( new File(fileDialog.getDirectory(),fileDialog.getFile()) );
                }
            }
        });
        fileMenu.add(open);

        JMenu mruMenu = new JMenuEx("Reopen");
        mruMenu.setMnemonic(KeyEvent.VK_R);
        mruMenuFactory = new MRUMenuFactory(MainFrame.class, mruMenu, 10, new MRUMenuFactory.Listener() {
            public boolean onFileSelected(File file) {
                return open(file);
            }
        });
        fileMenu.add(mruMenu);

        fileMenu.addSeparator();

        JMenuItem leftTabMenu = new JMenuItem("Select left tab");
        leftTabMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_MASK));
        leftTabMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int sel = tabbedPane.getSelectedIndex() - 1;
                if(sel>=0)
                    tabbedPane.setSelectedIndex(sel);
            }
        });
        fileMenu.add(leftTabMenu);

        JMenuItem rightTabMenu = new JMenuItem("Select right tab");
        rightTabMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_MASK));
        rightTabMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int sel = tabbedPane.getSelectedIndex() + 1;
                if(sel<tabbedPane.getTabCount())
                    tabbedPane.setSelectedIndex(sel);
            }
        });
        fileMenu.add(rightTabMenu);

        JMenuItem closeTabMenu = new JMenuItem("Close Tab");
        closeTabMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.CTRL_MASK));
        closeTabMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = tabbedPane.getSelectedIndex();
                Component c = tabbedPane.getComponentAt(idx);
                tabbedPane.removeTabAt(idx);
                c.setVisible(false);
            }
        });
        fileMenu.add(closeTabMenu);

        fileMenu.addSeparator();

        JMenuItem quit = new JMenuItem("Quit");
        quit.setMnemonic(KeyEvent.VK_X);
        quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
        quit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        fileMenu.add(quit);

        return fileMenu;
    }

    private JMenu createViewMenu() {
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        {
            JMenu timeUnitMenu = new JMenu("Unit of time");
            timeUnitMenu.setMnemonic('U');
            viewMenu.add(timeUnitMenu);

            RadioButtonMenuFactory<Integer> rbmf = new RadioButtonMenuFactory<Integer>(timeUnitMenu,
                new RadioButtonMenuFactory.Listener<Integer>() {
                    public void onSelected(Integer tag) {
                        tabbedPane.repaint();
                    }
                });
            rbmf.add(new JRadioButtonMenuItem("ms (10^-3)"),1000*1000);
            JRadioButtonMenuItem item = new JRadioButtonMenuItem("us (10^-6)");
            rbmf.add(item,1000);
            rbmf.add(new JRadioButtonMenuItem("ns (10^-9)"),1);
            item.setSelected(true); // don't forget to change the default of TimeRenderer if you change this.
        }

        JMenuItem hideMenu = new JMenuItem("Hide methods...");
        hideMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new HideThresholdDialog(MainFrame.this,viewConfig).setVisible(true);
            }
        });
        viewMenu.add(hideMenu);

        JMenuItem collapseMenu = new JMenuItem("Collapse methods...",KeyEvent.VK_C);
        collapseMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new CollapseMethodDialog(MainFrame.this,viewConfig).setVisible(true);
            }
        });
        viewMenu.add(collapseMenu);

        final JMenuItem useFullNameMenu = new JCheckBoxMenuItem("Show full class names");
        viewMenu.add(useFullNameMenu);
        useFullNameMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                useFullName = useFullNameMenu.isSelected();
            }
        });

        return viewMenu;
    }

    /**
     * Opens the specified profile data.
     *
     * @return true
     *      if the operation was successful. False if there was any error.
     *      The error has already been reported to the user.
     */
    boolean open(File file) {
        try {
            // sniff the contents
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            short s = dis.readShort();
            dis.close();

            if(s==(short)0xbeef) {
                // this is the raw file

                File out = Transformer.getDefaultOutput(file);
                if(out.exists()) {
                    if(out.lastModified() > file.lastModified()) {
                        // just open the condensed file
                        return open(out);
                    } else {
                        // ask to perform the transformation
                        if( JOptionPane.showConfirmDialog(this,
                            "The older condensed profiler output was found. Overwrite?","KProfiler",
                            JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION )
                            return false;   // cancel now
                    }
                }
                // transform method runs asynchronously.
                transform(file,out);
                return false;
            }

            ProfileData data = ProfileDataFactory.create(file);

            mruMenuFactory.addFile(file);
            ForwardTraceModel model = new ForwardTraceModel(data.getRoot());
            model.setFilter(viewConfig);
            open(file.getName(), model);

            return true;
        } catch (IOException e) {
            Logger.global.log(Level.INFO,"Failed to open "+file,e);
            JOptionPane.showMessageDialog(this,e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);

            return false;
        }
    }

    public void open(String title, ModelNavigator model) {
        CallTree treeTable = new CallTree(this);

        JScrollPane scrollPane = new JScrollPane(treeTable);
//        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        scrollPane.getViewport().setBackground(treeTable.getBackground());
        tabbedPane.addTab(title, scrollPane);

        treeTable.setModel(model);
    }

    /**
     * Gets the currently active {@link CallTree} component.
     *
     * @return
     *      null if none is active.
     */
    public CallTree getCurrentCallTree() {
        return (CallTree)tabbedPane.getSelectedComponent();
    }

    public void transform(final File src, final File dst) throws IOException {
        final RawDataFile rd = new RawDataFile(src);

        final int classSize = rd.getClassStream().getSize();
        final int size = classSize + rd.getThreadStream(0).getSize();

        final ProgressMonitor pm = new ProgressMonitor(MainFrame.this,
              "Processing the raw output", "", 0, size);
        pm.setMillisToPopup(500);
        pm.setMillisToDecideToPopup(500);

        new Thread() {
            public void run() {
                Monitor monitor = new Monitor(pm);

                try {
                    MethodInfoDictionary dic = new MethodInfoDictionary(rd,monitor);

                    Transformer transformer = new Transformer(dic);
                    monitor.setBase(classSize);
                    transformer.setMonitor(monitor);
                    transformer.transform(rd.getThreadStream(0));
                    transformer.write(dst);

                    // open this newly created file.
                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            open(dst);
                        }
                    });
                } catch (AbortException e) {
                    // aborted by the user
                    return;
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                        e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
                } catch (OutOfMemoryError e) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                        "Not Enough Memory. Please use -Xmx to increase the memory size","Error",JOptionPane.ERROR_MESSAGE);
                } finally {
                    pm.close();
                }
            }
        }.start();
    }

    private class Monitor implements net.java.dev.profiler.kprofiler.ProgressMonitor {
        private final ProgressMonitor monitor;

        private int base;

        public Monitor(ProgressMonitor monitor) {
            this.monitor = monitor;
        }

        public void setBase(int base) {
            this.base = base;
        }


        public void progress(final int current, int total) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    monitor.setProgress(base+current);
                }
            });

            if(monitor.isCanceled())
                throw new AbortException();
        }
    }

    private static class AbortException extends RuntimeException {
    }
}
