package net.java.dev.profiler.kprofiler.viewer;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.java.dev.profiler.kprofiler.viewer.ui_util.JDialogEx;
import net.java.dev.profiler.kprofiler.viewer.model.ViewConfig;

import javax.swing.*;
import java.awt.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class HideThresholdDialog extends JDialogEx {

    private final JCheckBox enabled = new JCheckBox("Hide a method if it takes less than N% of its parent to execute");
    private final JTextField ratio = new JFormattedTextField();

    private final ViewConfig config;

    public HideThresholdDialog(Frame owner,ViewConfig config) {
        super(owner,"Hide methods");

        this.config = config;

        ratio.setColumns(3);
        ratio.setText(Integer.toString(config.getThreshold()));

        enabled.setSelected(config.isEngaged());

        FormLayout layout = new FormLayout(
            "pref:grow, 3dlu, pref, 3dlu, pref", // columns
            "pref, 3dlu, pref, 7dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();
        builder.add(enabled,   cc.xyw(1,1,5));
        builder.addLabel("threshold:", cc.xy(1,3,CellConstraints.RIGHT,CellConstraints.DEFAULT));
        builder.add(ratio, cc.xy(3,3));
        builder.addLabel("%",cc.xy(5,3));
        builder.add(ButtonBarFactory.buildOKCancelBar(okButton,cancelButton),cc.xyw(1,5,5));

        add(builder.getPanel());

        pack();
        setLocationRelativeTo(owner);
    }

    protected boolean apply() {
        try {
            config.setThreshold(Integer.parseInt(ratio.getText()));
            config.setEngaged(enabled.isSelected());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,ratio.getText()+" is not a number","KProfiler",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
}
