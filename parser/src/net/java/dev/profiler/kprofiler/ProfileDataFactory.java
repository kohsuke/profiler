package net.java.dev.profiler.kprofiler;

import net.java.dev.profiler.kprofiler.impl.ProfileDataImpl;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public final class ProfileDataFactory {
    private ProfileDataFactory() {} // no instanciation

    /**
     * Parses a data file into the profile data.
     *
     * @throws IOException
     *      if the data file is corrupt, wrong, or any errors reading a file.
     */
    public static ProfileData create( File data ) throws IOException {
        return new ProfileDataImpl(data);
    }
}
