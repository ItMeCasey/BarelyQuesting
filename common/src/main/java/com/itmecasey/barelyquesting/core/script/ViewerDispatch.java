package com.itmecasey.barelyquesting.core.script;

import java.util.logging.Logger;

/** Each optional viewer fails independently so another installed viewer can still handle the click. */
public final class ViewerDispatch {
    private static final Logger LOG = Logger.getLogger("barelyquesting");
    private ViewerDispatch() { }

    @FunctionalInterface
    public interface Attempt {
        boolean open() throws ReflectiveOperationException;
    }

    public static boolean first(Attempt... viewers) {
        for (Attempt viewer : viewers) {
            try {
                if (viewer.open()) return true;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                LOG.warning("Barely Questing: optional viewer failed; trying the next viewer: " + e);
            }
        }
        return false;
    }
}
