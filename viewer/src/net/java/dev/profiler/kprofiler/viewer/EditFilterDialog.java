package net.java.dev.profiler.kprofiler.viewer;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.java.dev.profiler.kprofiler.viewer.ui_util.JDialogEx;
import net.java.dev.profiler.kprofiler.viewer.model.FilterConfig;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Kohsuke Kawaguchi
 */
public class EditFilterDialog extends JDialogEx {
    /**
     * The filter we ar eediting
     */
    private final FilterConfig filter;

    private final JTextField name = new JTextField();
    private final JTextArea mask = new JTextArea(8,60);

    public EditFilterDialog(Frame owner, FilterConfig filter) throws HeadlessException {
        super(owner,"Edit");
        this.filter = filter;
        name.setText(filter.getName());
        mask.setText(filter.getMask());

        FormLayout layout = new FormLayout(
            "right:pref, 3dlu, default:grow", // columns
            "pref, 3dlu, top:default:grow, 7dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();
        builder.addLabel("&Name:",   cc.xy(1,1)).setLabelFor(name);
        builder.add(name, cc.xy(3,1));
        builder.addLabel("&Pattern:",cc.xy(1,3)).setLabelFor(mask);
        builder.add(new JScrollPane(mask), cc.xy(3,3,CellConstraints.FILL,CellConstraints.FILL));
        builder.add(ButtonBarFactory.buildOKCancelBar(okButton,cancelButton),cc.xyw(1,5,3));

        add(builder.getPanel());

        mask.setLineWrap(true);

        pack();
        setLocationRelativeTo(owner);

        // set the focus into the right place.
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                JTextComponent c;
                if(name.getText().length()==0)
                    c = name;
                else
                    c = mask;
                c.requestFocusInWindow();
                c.selectAll();
            }
        });
    }



    protected boolean apply() {
        filter.setName(name.getText());
        filter.setMask(mask.getText());
        return true;
    }
}
