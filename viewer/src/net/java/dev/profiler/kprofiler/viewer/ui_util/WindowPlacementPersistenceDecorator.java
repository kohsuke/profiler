package net.java.dev.profiler.kprofiler.viewer.ui_util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Handles the persistence of the window location/size.
 *
 * @author Kohsuke Kawaguchi
 */
public final class WindowPlacementPersistenceDecorator {
    private final Point pos = new Point(0, 0);
    private final Dimension dim = new Dimension(400, 200);

    private final JFrame frame;

    private final String prefix;

    /**
     * Decorates the given {@link JFrame} so that its location
     * will be remembered.
     *
     * <p>
     * Call this method from the constructor of your {@link JFrame}.
     */
    public static void decorate(JFrame frame) {
        new WindowPlacementPersistenceDecorator(frame).initLocation();
    }

    private WindowPlacementPersistenceDecorator(JFrame _frame) {
        this.frame = _frame;
        this.prefix = frame.getClass().getName();

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                saveLocation();
            }
        });
        frame.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                JFrame frm = (JFrame) e.getSource();
                if (frm.getExtendedState() == JFrame.NORMAL) {
                    try {
                        pos.setLocation(frm.getLocationOnScreen());
                    } catch (IllegalComponentStateException icse) {
                    }
                }
            }

            public void componentResized(ComponentEvent e) {
                JFrame frm = (JFrame) e.getSource();
                if (frm.getExtendedState() == JFrame.NORMAL) {
                    dim.setSize(frame.getSize());
                }
            }
            //void componentHidden(ComponentEvent e){}
            //void componentShown(ComponentEvent e)
        });
    }

    private Preferences getPreferences() {
        return Preferences.userNodeForPackage(frame.getClass());
    }

    private void initLocation() {
        Preferences lprefs = getPreferences();

        pos.x = lprefs.getInt(prefix + "locx", -1);
        pos.y = lprefs.getInt(prefix + "locy", -1);
        if(pos.x==-1 || pos.y==-1)
            frame.setLocationByPlatform(true);
        else
            frame.setLocation(pos.x,pos.y);
        int wdim = lprefs.getInt(prefix + "dimw", dim.width);
        int hdim = lprefs.getInt(prefix + "dimh", dim.height);
        dim.setSize(wdim, hdim);
        frame.setPreferredSize(dim);
    }

    private void saveLocation() {
        Preferences lprefs = getPreferences();

        lprefs.putInt(prefix + "locx", pos.x);
        lprefs.putInt(prefix + "locy", pos.y);
        lprefs.putInt(prefix + "dimw", dim.width);
        lprefs.putInt(prefix + "dimh", dim.height);
        try {
            lprefs.flush();
        } catch (java.util.prefs.BackingStoreException e) {
            Logger.global.info("Unable to save the location information");
        }
    }
}
