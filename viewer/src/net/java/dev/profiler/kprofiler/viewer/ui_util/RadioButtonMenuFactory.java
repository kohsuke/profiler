package net.java.dev.profiler.kprofiler.viewer.ui_util;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Kohsuke Kawaguchi
 */
public class RadioButtonMenuFactory<T> implements ActionListener {
    private final ButtonGroup group = new ButtonGroup();

    private final JMenu parent;

    // I can't believe I have to do this!
    private final Map<JRadioButtonMenuItem,T> tags = new HashMap<JRadioButtonMenuItem, T>();

    public interface Listener<T> {
        void onSelected(T tag);
    }

    private Listener<T> listener;

    public RadioButtonMenuFactory(JMenu parent,Listener<T> listener) {
        this.parent = parent;
        this.listener = listener;
    }

    public Listener<T> getListener() {
        return listener;
    }

    public void setListener(Listener<T> listener) {
        this.listener = listener;
    }

    /**
     * Adds the radio menu item to the parent {@link JMenu}.
     *
     * @param tag
     *      This tag will be reported when this menu item is selected.
     */
    public void add(JRadioButtonMenuItem item, T tag) {
        group.add(item);
        parent.add(item);
        item.addActionListener(this);
        tags.put(item,tag);
    }

    public void actionPerformed(ActionEvent e) {
        T tag = tags.get(e.getSource());
        if(listener!=null)
            listener.onSelected(tag);
    }
}
