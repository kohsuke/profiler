package net.java.dev.profiler.kprofiler.viewer.ui_util;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Manges a menu of "most recently used" files.
 *
 * @author Kohsuke Kawaguchi
 */
public class MRUMenuFactory implements MenuListener {
    private final Class key;
    private final Preferences preferences;
    private final int maxListSize;
    private final JMenu menu;

    /**
     * Used to store the MRU list.
     */
    private final List<File> mruList = new ArrayList<File>();
    private Listener listener;

    /**
     * Receives a notification when a file from MRU list is selected.
     */
    public interface Listener {
        /**
         * Fired when a file is selected.
         *
         * @return
         *      true to move this file to the top of MRU.
         *      false to remove this file from the MRU.
         */
        boolean onFileSelected(File file);
    }

    /**
     * @param key
     *      Used to identify the persistent data storage
     *      that keeps the MRU list.
     *      <p>
     *      Pass in the same class,
     *      you get the {@link MRUMenuFactory} connected
     *      to the same data store. Pass in a different class,
     *      you get the {@link MRUMenuFactory} connceted
     *      to the different data store.
     *      <p>
     *      Typically you should pass in one of your application
     *      class so that the persisted MRU list isn't mixed up
     *      with some other applications.
     *
     * @param menu
     *      This menu will be populated by the MRU list.
     *      The menu shouldn't contain any other items.
     */
    public MRUMenuFactory(Class key, JMenu menu, int maxListSize, Listener listener ) {
        this.key = key;
        preferences = Preferences.userNodeForPackage(key);
        this.menu = menu;
        this.maxListSize = maxListSize;

        // this allows us to update the enable/disable state
        // of the menu right before when it's displayed.
        menu.setModel(new DefaultButtonModel() {
            private boolean enabled;
            public boolean isEnabled() {
                load();
                boolean old = enabled;
                enabled = !mruList.isEmpty();
                if( old != enabled )
                    fireStateChanged();
                return enabled;
            }
        });

        menu.addMenuListener(this);

        setListener(listener);
    }

    public Listener getListener() {
        return listener;
    }

    /**
     * Sets the {@link Listener} which gets notified when
     * the user selects a file from the list.
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Adds the specified file into the MRU list.
     */
    public void addFile( File f ) {
        load();
        f = f.getAbsoluteFile();
        mruList.remove(f);      // remove the duplicate if any
        mruList.add(0,f);
        while(mruList.size()>maxListSize)
            mruList.remove(mruList.size()-1);
        save();
    }

    /**
     * Removes the specified file from the MRU.
     */
    public void removeFile( File f ) {
        load();
        mruList.remove(f.getAbsoluteFile());
        save();
    }

    /**
     * Loads the MRU list from the persistent storage into {@link #mruList}.
     */
    private void load() {
        mruList.clear();
        int n = preferences.getInt(key.getName() + ".mru.size",0);
        for( int i=0; i<n; i++ ) {
            String fileName = preferences.get(key.getName() + ".mru." + i, null);
            if(fileName!=null) {
                File f = new File(fileName);
                if(f.exists())
                    mruList.add(f);
            }
        }
    }

    /**
     * Saves the MRU list to the persistent storage from {@link #mruList}.
     */
    private void save() {
        preferences.putInt(key.getName()+".mru.size",mruList.size());
        for( int i=0; i<mruList.size(); i++ ) {
            preferences.put(key.getName()+".mru."+i, mruList.get(i).getPath() );
        }
    }

    /**
     * Reloads the body of the menu.
     */
    public void menuSelected(MenuEvent e) {
        load();

        menu.removeAll();
        for( final File f : mruList ) {
            JMenuItem mi = new JMenuItem(f.getPath());
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(listener!=null) {
                        if(listener.onFileSelected(f))
                            addFile(f);
                        else
                            removeFile(f);
                    }
                }
            });
            menu.add(mi);
        }
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuCanceled(MenuEvent e) {
    }
}
