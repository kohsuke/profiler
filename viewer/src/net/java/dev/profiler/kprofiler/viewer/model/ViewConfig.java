package net.java.dev.profiler.kprofiler.viewer.model;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import net.java.dev.profiler.kprofiler.MethodCall;
import net.java.dev.profiler.kprofiler.viewer.ui_util.TypedEventListeners;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * View configuration.
 *
 * <p>
 * This data is persisted to {@link Preferences} and kept globally.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ViewConfig implements MethodCallFilter {
    private int threshold = 3;
    private boolean engaged = true;

    private List<FilterConfig> filters = new ArrayList<FilterConfig>();

    public interface ChangeListener extends EventListener {
        void onChanged();
    }

    private transient final TypedEventListeners<ChangeListener> eventListeners =
            new TypedEventListeners<ChangeListener>(ChangeListener.class);

    private ViewConfig() {
        filters.add(new FilterConfig(true,"JUnit","junit.*"));
        filters.add(new FilterConfig(true,"Apache","org.apache.*"));
    }

    /**
     * Retrieves the persisted {@link ViewConfig} instance.
     */
    public static ViewConfig create() {
        ViewConfig vc = new ViewConfig();
        try {
            XStream xs = createXStream();
            File configFile = getConfigFile();
            if(configFile.exists()) {
                InputStreamReader in = new InputStreamReader(new FileInputStream(configFile), "UTF-8");
                xs.unmarshal(new DomDriver().createReader(in),vc);
            }
            return vc;
        } catch (Exception e) {
            e.printStackTrace();
            return vc;
        }
    }

    private void save() {
        eventListeners.getSink().onChanged();
        try {
            createXStream().toXML(this,new OutputStreamWriter(new FileOutputStream(getConfigFile()),"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // impossible
            throw new Error(e);
        } catch (FileNotFoundException e) {
            // just ignore.
        }
    }

    private static File getConfigFile() {
        return new File(System.getProperty("user.home"),".kprofiler.view");
    }

    private static XStream createXStream() {
        XStream xs = new XStream(new DomDriver());
        xs.alias("config",ViewConfig.class);
        xs.alias("filter",FilterConfig.class);
        return xs;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
        save();
    }

    public boolean isEngaged() {
        return engaged;
    }

    public void setEngaged(boolean engaged) {
        this.engaged = engaged;
        save();
    }

    public List<FilterConfig> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterConfig> filters) {
        this.filters = filters;
        save();
    }

    public void addChangeListener(ChangeListener cl) {
        eventListeners.add(cl);
    }

    public void removeChangeListener(ChangeListener cl) {
        eventListeners.remove(cl);
    }

    public boolean shallBeCollapsed(MethodCall parent, MethodCall child) {
        if(parent.method()==null)    return false;

        for( FilterConfig fc : filters )
            if(fc.shallBeCollapsed(parent,child))
                return true;
        return false;
    }
}
