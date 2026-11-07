package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import java.util.ArrayList;
import java.util.List;

/** Recipe viewer bridge (filled in by the viewer step). */
public final class Viewers {
    private Viewers() {
    }

    public static boolean show(ItemSpec item, boolean uses) {
        return false;
    }

    public static List<String> installed() {
        return new ArrayList<>();
    }
}
