#!/usr/bin/env python3
"""Draws the original textures of the Parchment theme (pixel art, no Minecraft textures) and writes the theme files.

Run from the repository root: python scripts/make_theme_assets.py
Output: common/src/main/resources/assets/barelyquesting/textures/gui/parchment/*.png
"""
import json
import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main", "resources", "assets", "barelyquesting")
TEX = os.path.join(ROOT, "textures", "gui", "parchment")
THEMES = os.path.join(ROOT, "barelyquesting", "themes")
os.makedirs(TEX, exist_ok=True)
os.makedirs(THEMES, exist_ok=True)

PAPER = (232, 213, 168, 255)
PAPER_LIGHT = (244, 232, 196, 255)
PAPER_DARK = (205, 181, 130, 255)
INK = (59, 42, 26, 255)
EDGE = (139, 107, 61, 255)
EDGE_DARK = (92, 66, 36, 255)


def save(img, name):
    img.save(os.path.join(TEX, name + ".png"))


def frame(size, border, fill, light, dark, ink=INK):
    """A bordered square: ink outline, light top-left bevel, dark bottom-right bevel."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, size - 1, size - 1], fill=ink)
    d.rectangle([1, 1, size - 2, size - 2], fill=dark)
    d.rectangle([1, 1, size - 3, size - 3], fill=light)
    d.rectangle([2, 2, size - 3, size - 3], fill=fill)
    # corner notches for a worn look
    for x, y in ((0, 0), (size - 1, 0), (0, size - 1), (size - 1, size - 1)):
        img.putpixel((x, y), (0, 0, 0, 0))
    return img


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


# panels
save(frame(32, 6, PAPER, PAPER_LIGHT, EDGE), "panel")
inset = frame(32, 6, PAPER_DARK, EDGE, PAPER_LIGHT)
save(inset, "panel_inset")
save(frame(16, 4, PAPER_LIGHT, (255, 248, 222, 255), EDGE), "button")
save(frame(16, 4, (255, 245, 214, 255), (255, 252, 236, 255), EDGE), "button_hover")
save(frame(16, 4, PAPER_DARK, PAPER, EDGE), "button_off")
save(frame(16, 4, PAPER, PAPER_LIGHT, EDGE), "tab")
save(frame(16, 4, (250, 226, 150, 255), (255, 244, 200, 255), EDGE), "tab_selected")
save(frame(16, 3, (190, 166, 116, 255), EDGE_DARK, (214, 192, 142, 255)), "slot")
save(frame(16, 3, (96, 72, 40, 255), EDGE_DARK, (120, 94, 56, 255)), "bar")
save(frame(16, 3, (122, 168, 70, 255), (170, 210, 110, 255), (80, 120, 40, 255)), "bar_fill")

# nodes: a medallion per state
STATES = {
    "locked": (150, 140, 124, 255),
    "available": (110, 150, 196, 255),
    "progress": (226, 178, 62, 255),
    "complete": (118, 176, 82, 255),
    "claimed": (74, 128, 70, 255),
}
for name, color in STATES.items():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, 31, 31], radius=7, fill=INK)
    d.rounded_rectangle([2, 2, 29, 29], radius=6, fill=shade(color, 0.7))
    d.rounded_rectangle([3, 3, 28, 28], radius=5, fill=color)
    d.rounded_rectangle([6, 6, 25, 25], radius=4, fill=shade(color, 1.18))
    d.line([(5, 4), (26, 4)], fill=shade(color, 1.35), width=1)
    save(img, "node_" + name)


def sprite(name, size, border):
    return {"texture": "barelyquesting:textures/gui/parchment/%s.png" % name, "size": [size, size], "border": [border] * 4}


parchment = {
    "name": "Parchment",
    "author": "ItMeCasey",
    "font": "",
    "node_size": 28,
    "colors": {
        "background": "#c0201408",
        "panel": "#e8d5a8", "panel_edge": "#8b6b3d", "panel_inset": "#cdb582", "header": "#d8c08a",
        "text": "#3b2a1a", "text_muted": "#7a6444", "title": "#5c2e0e", "accent": "#9c5a14",
        "button": "#f4e8c4", "button_hover": "#fff5d6", "button_off": "#cdb582",
        "tab": "#e8d5a8", "tab_selected": "#fae296", "slot": "#bea674", "bar": "#604828", "bar_fill": "#7aa846",
        "line": "#7a6444", "line_done": "#4a8046",
        "node_locked": "#968c7c", "node_available": "#6e96c4", "node_progress": "#e2b23e", "node_complete": "#76b052", "node_claimed": "#4a8046",
        "node_selected": "#3b2a1a", "danger": "#a82a1c", "success": "#2e7a30",
    },
    "sprites": {
        "panel": sprite("panel", 32, 6), "panel_inset": sprite("panel_inset", 32, 6),
        "button": sprite("button", 16, 4), "button_hover": sprite("button_hover", 16, 4), "button_off": sprite("button_off", 16, 4),
        "tab": sprite("tab", 16, 4), "tab_selected": sprite("tab_selected", 16, 4),
        "slot": sprite("slot", 16, 3), "bar": sprite("bar", 16, 3),
        **{"node_" + n: {"texture": "barelyquesting:textures/gui/parchment/node_%s.png" % n, "size": [32, 32], "border": [0, 0, 0, 0]} for n in STATES},
    },
    "sounds": {
        "click": "minecraft:item.book.page_turn", "complete": "minecraft:entity.player.levelup",
        "claim": "minecraft:entity.experience_orb.pickup", "open": "minecraft:item.book.page_turn",
    },
}
default = {
    "name": "Default",
    "author": "ItMeCasey",
    "font": "",
    "node_size": 26,
    "colors": {
        "background": "#c0101018", "panel": "#2b2b33", "panel_edge": "#55556a", "panel_inset": "#1b1b22", "header": "#22222b",
        "text": "#ededf5", "text_muted": "#a0a0b4", "title": "#ffe28c", "accent": "#f2b33d",
        "button": "#3a3a48", "button_hover": "#4c4c60", "button_off": "#26262e", "tab": "#2b2b33", "tab_selected": "#45455a",
        "slot": "#15151b", "bar": "#15151b", "bar_fill": "#4fc36b", "line": "#6a6a80", "line_done": "#4fc36b",
        "node_locked": "#3a3a44", "node_available": "#4a6fa5", "node_progress": "#c9a227", "node_complete": "#4fc36b", "node_claimed": "#2e7d46",
        "node_selected": "#ffffff", "danger": "#e05252", "success": "#4fc36b",
    },
    "sprites": {},
    "sounds": {
        "click": "minecraft:ui.button.click", "complete": "minecraft:entity.player.levelup",
        "claim": "minecraft:entity.experience_orb.pickup", "open": "minecraft:item.book.page_turn",
    },
}
with open(os.path.join(THEMES, "parchment.json"), "w", encoding="utf8") as f:
    json.dump(parchment, f, indent=2)
    f.write("\n")
with open(os.path.join(THEMES, "default.json"), "w", encoding="utf8") as f:
    json.dump(default, f, indent=2)
    f.write("\n")
with open(os.path.join(THEMES, "index.json"), "w", encoding="utf8") as f:
    json.dump({"themes": ["default", "parchment"]}, f, indent=2)
    f.write("\n")
print("wrote", len(os.listdir(TEX)), "textures and 3 theme files")
