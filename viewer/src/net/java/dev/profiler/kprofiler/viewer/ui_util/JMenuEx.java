package net.java.dev.profiler.kprofiler.viewer.ui_util;

import javax.swing.*;

/**
 * Fixes a problem of {@link JMenu} where it shows an empty little box
 * even if the menu is disabled.
 *
 * @author Kohsuke Kawaguchi
 */
public class JMenuEx extends JMenu {
    public JMenuEx() {
    }

    public JMenuEx(String s) {
        super(s);
    }

    public JMenuEx(Action a) {
        super(a);
    }

    public JMenuEx(String s, boolean b) {
        super(s, b);
    }

    public void setPopupMenuVisible(boolean b) {
        if(b && super.getMenuComponents().length==0)
            return; // don't show the menu if it's empty
        else
            super.setPopupMenuVisible(b);
    }
}
