package net.java.dev.profiler.kprofiler.viewer.model;

import net.java.dev.profiler.kprofiler.MethodCall;

import java.util.regex.Pattern;

/**
 * @author Kohsuke Kawaguchi
 */
public class FilterConfig implements MethodCallFilter {
    private boolean engaged;
    private String name;
    private String mask;
    private transient Pattern pattern;

    public FilterConfig() {
    }

    public FilterConfig(boolean engaged, String name, String mask) {
        this.engaged = engaged;
        this.name = name;
        this.mask = mask;
    }

    public void setEngaged(boolean engaged) {
        this.engaged = engaged;
    }

    public void setMask(String mask) {
        this.mask = mask;
        pattern = null;
    }

    private Pattern getPattern() {
        if(pattern==null)
            pattern = Pattern.compile(mask.replace(".","\\.").replace("*",".+").replace(",","|"));
        return pattern;
    }

    public boolean isEngaged() {
        return engaged;
    }

    public String getMask() {
        return mask;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean shallBeCollapsed(MethodCall parent, MethodCall child) {
        Pattern pattern = getPattern();
        return engaged && pattern.matcher(child.method().fullName()).matches()
                      && pattern.matcher(parent.method().fullName()).matches();
    }

    public FilterConfig clone() {
        return new FilterConfig(engaged,name,mask);
    }
}
