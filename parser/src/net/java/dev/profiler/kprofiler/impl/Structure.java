package net.java.dev.profiler.kprofiler.impl;

import net.java.dev.profiler.kprofiler.MethodCall;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class Structure {
    /**
     * The data file to which this cursor belongs.
     */
    protected final ProfileDataImpl owner;

    /**
     * Gets the current position.
     */
    protected final int index;

    public Structure(ProfileDataImpl owner, int index) {
        this.owner = owner;
        this.index = index;
    }

    protected abstract int getInt(int offset);

    protected final long getLong(int offset) {
        return (((long)getInt(offset))<<32)|(((long)getInt(offset+1))&0xFFFFFFFFL);
    }

    /**
     * Gets the string from the constant pool.
     */
    protected final String getString(int offset) {
        return owner.getConstantPool(getInt(offset));
    }

    protected final MethodCall getMethodCall(int offset) {
        int i = getInt(offset);
        if(i==-1)   return null;
        else        return new MethodCallImpl(owner,i);
    }
}
