"""One-off: route screen access in version-26.1.2 through McGui so version modules can differ."""
import glob
import re

SRC = '''package com.itmecasey.barelyquesting.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Client only: the open screen. Its home on Minecraft moved between versions, so every version module has its own copy. */
public final class McGui {
    private McGui() {
    }

    public static Screen screen() {
        return Minecraft.getInstance().screen;
    }

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }
}
'''
open('version-26.1.2/src/main/java/com/itmecasey/barelyquesting/compat/McGui.java', 'w').write(SRC)
for f in glob.glob('version-26.1.2/src/**/*.java', recursive=True):
    if f.endswith('McGui.java'):
        continue
    s = open(f, encoding='utf8').read()
    o = s
    s = re.sub(r'\bmc\.screen\b', 'McGui.screen()', s)
    s = s.replace('mc.setScreen(', 'McGui.setScreen(')
    if s != o:
        if '/dev/' in f.replace(chr(92), '/') and 'compat.McGui;' not in s:
            s = s.replace('import com.itmecasey.barelyquesting.compat.QuestScreen;',
                          'import com.itmecasey.barelyquesting.compat.McGui;\nimport com.itmecasey.barelyquesting.compat.QuestScreen;')
        open(f, 'w', encoding='utf8').write(s)
        print(f)
