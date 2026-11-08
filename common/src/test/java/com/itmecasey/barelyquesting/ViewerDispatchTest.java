package com.itmecasey.barelyquesting;

import com.itmecasey.barelyquesting.core.script.ViewerDispatch;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ViewerDispatchTest {
    @Test void missingViewerFallsThrough() {
        assertTrue(ViewerDispatch.first(() -> false, () -> true));
    }
    @Test void incompatibleViewersDoNotBlockFallback() {
        assertTrue(ViewerDispatch.first(
                () -> { throw new NoSuchMethodException("changed API"); },
                () -> { throw new IllegalArgumentException("wrong signature"); },
                () -> { throw new NoClassDefFoundError("optional transitive dependency"); },
                () -> true));
    }
    @Test void successfulViewerStopsDispatch() {
        assertTrue(ViewerDispatch.first(() -> true, () -> { fail("second viewer must not open"); return false; }));
    }
    @Test void noWorkingViewerReturnsFalse() {
        assertFalse(ViewerDispatch.first(() -> false, () -> { throw new IllegalStateException("not ready"); }));
    }
}
