package net.java.dev.profiler.kprofiler.viewer.ui_util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Improved {@link JDialog} class with the standard OK/cancel button.
 * @author Kohsuke Kawaguchi
 */
public abstract class JDialogEx extends JDialog {

    protected final JButton okButton = new JButton("OK");
    protected final JButton cancelButton = new JButton("Cancel");

    private int returnCode = JOptionPane.CANCEL_OPTION;

    public JDialogEx(Frame owner, String title) throws HeadlessException {
        super(owner,title,true);

        okButton.setMnemonic('O');
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(apply()) {
                    returnCode = JOptionPane.OK_OPTION;
                    dispose();
                }
            }
        });
        getRootPane().setDefaultButton(okButton);

        ActionListener cancelAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                returnCode = JOptionPane.CANCEL_OPTION;
                dispose();
            }
        };
        cancelButton.addActionListener(cancelAction);
        cancelButton.setMnemonic('C');

        getRootPane().registerKeyboardAction(cancelAction,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Called when the OK/apply button is pressed.
     *
     * @return true to close the dialog, false to prevent it.
     */
    protected abstract boolean apply();

    /**
     * Called when the dialog is cancelled.
     */
    protected void cancel() {
    }

    /**
     *
     * @return
     *      either {@link JOptionPane#OK_OPTION} or {@link JOptionPane#CANCEL_OPTION}.
     */
    public int getReturnCode() {
        return returnCode;
    }

}
