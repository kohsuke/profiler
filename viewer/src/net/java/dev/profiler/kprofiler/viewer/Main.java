package net.java.dev.profiler.kprofiler.viewer;

import com.jgoodies.plaf.Options;
import com.jgoodies.plaf.plastic.PlasticXPLookAndFeel;

import javax.swing.*;
import java.io.IOException;
import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(final String[] args) throws IOException {
        Options.setPopupDropShadowEnabled(true);
//        ClearLookManager.setMode(ClearLookMode.DEBUG);

        try {
           UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        } catch (Exception e) {}
//        String nativeLF = UIManager.getSystemLookAndFeelClassName();
//        UIManager.setLookAndFeel(nativeLF);

        MainFrame frame = new MainFrame();
        frame.setVisible(true);
        for( String arg : args )
            frame.open(new File(arg));
    }
}
