#!/usr/bin/env python3
"""Draws the Barely Questing icon and listing banners (original pixel art, studio palette). Needs Pillow.

Run from the repository root: python branding/source/make_branding.py
Writes branding/icon/*.png and branding/banners/*.png.
"""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ICON = os.path.join(ROOT, "icon")
BANNERS = os.path.join(ROOT, "banners")
os.makedirs(ICON, exist_ok=True)
os.makedirs(BANNERS, exist_ok=True)

NIGHT = (15, 14, 26)
NIGHT2 = (26, 24, 48)
EDGE = (46, 42, 70)
GOLD = (242, 179, 61)
GOLD_LIGHT = (255, 226, 140)
GOLD_DARK = (178, 116, 32)
BLUE = (122, 168, 255)
TEXT = (244, 242, 255)
MUTED = (169, 164, 200)
PAPER = (232, 213, 168)
PAPER_DARK = (205, 181, 130)
INK = (59, 42, 26)
GREEN = (118, 176, 82)


def icon32():
    """A quest scroll: a night tile, a parchment sheet with a node graph on it and a gold star."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # tile with stepped corners
    d.rectangle([1, 0, 30, 31], fill=EDGE)
    d.rectangle([0, 1, 31, 30], fill=EDGE)
    d.rectangle([1, 1, 30, 30], fill=NIGHT2)
    d.rectangle([2, 2, 29, 29], fill=NIGHT)
    # parchment
    d.rectangle([6, 4, 25, 27], fill=INK)
    d.rectangle([7, 5, 24, 26], fill=PAPER)
    d.rectangle([7, 5, 24, 6], fill=(250, 236, 200))
    d.rectangle([7, 25, 24, 26], fill=PAPER_DARK)
    # rolled ends
    d.rectangle([5, 3, 26, 5], fill=INK)
    d.rectangle([6, 4, 25, 4], fill=GOLD_LIGHT)
    d.rectangle([5, 26, 26, 28], fill=INK)
    d.rectangle([6, 27, 25, 27], fill=GOLD_DARK)
    # quest graph: three nodes and lines
    d.line([(11, 21), (11, 15)], fill=INK)
    d.line([(11, 15), (20, 15)], fill=INK)
    d.line([(20, 15), (20, 10)], fill=INK)
    for (x, y, c) in ((11, 21, BLUE), (11, 15, GREEN), (20, 15, GREEN)):
        d.rectangle([x - 2, y - 2, x + 2, y + 2], fill=INK)
        d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=c)
    # gold star at the end of the path
    sx, sy = 20, 9
    for dx, dy in ((0, -3), (0, 3), (-3, 0), (3, 0)):
        d.point((sx + dx, sy + dy), fill=GOLD_DARK)
    d.rectangle([sx - 2, sy - 1, sx + 2, sy + 1], fill=GOLD)
    d.rectangle([sx - 1, sy - 2, sx + 1, sy + 2], fill=GOLD)
    d.point((sx, sy), fill=GOLD_LIGHT)
    return img


def font(size, bold=False):
    for name in ("consolab.ttf" if bold else "consola.ttf", "arialbd.ttf" if bold else "arial.ttf", "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"):
        for base in (r"C:\Windows\Fonts", "/usr/share/fonts/truetype/dejavu"):
            p = os.path.join(base, name)
            if os.path.exists(p):
                return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def background(w, h):
    img = Image.new("RGB", (w, h), NIGHT)
    px = img.load()
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(NIGHT[i] * (1 - t) + NIGHT2[i] * t) for i in range(3))
        for x in range(w):
            px[x, y] = c
    d = ImageDraw.Draw(img)
    step = max(24, w // 40)
    for x in range(0, w, step):
        for y in range(0, h, step):
            d.point((x, y), fill=EDGE)
    return img


def draw_graph(d, x, y, scale):
    """A small node graph on the right side of a banner."""
    nodes = [(0, 0), (2, 0), (4, 0), (4, 1), (6, -1), (6, 1)]
    state = [GREEN, GREEN, GOLD, BLUE, BLUE, BLUE]
    lines = [(0, 1), (1, 2), (2, 3), (2, 4), (3, 5)]
    pos = [(x + nx * scale, y + ny * scale) for nx, ny in nodes]
    for a, b in lines:
        (ax, ay), (bx, by) = pos[a], pos[b]
        mid = (ax + bx) // 2
        d.line([(ax, ay), (mid, ay), (mid, by), (bx, by)], fill=MUTED, width=max(2, scale // 12))
    for (px_, py_), c in zip(pos, state):
        r = scale * 0.42
        d.rounded_rectangle([px_ - r, py_ - r, px_ + r, py_ + r], radius=r * 0.3, fill=INK)
        r2 = r * 0.82
        d.rounded_rectangle([px_ - r2, py_ - r2, px_ + r2, py_ + r2], radius=r2 * 0.3, fill=c)


def banner(name, w, h, title, subtitle, graph=True):
    img = background(w, h)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, w - 1, h - 1], outline=EDGE, width=max(2, h // 60))
    tile = icon32().resize((int(h * 0.62),) * 2, Image.NEAREST)
    ox = int(h * 0.19)
    img.paste(tile, (ox, (h - tile.height) // 2), tile)
    tx = ox + tile.width + int(h * 0.12)
    ft = font(int(h * 0.26), True)
    fs = font(int(h * 0.11))
    d.text((tx + 3, int(h * 0.2) + 3), title, font=ft, fill=(0, 0, 0))
    d.text((tx, int(h * 0.2)), title, font=ft, fill=GOLD_LIGHT)
    d.text((tx, int(h * 0.62)), subtitle, font=fs, fill=TEXT)
    if graph:
        draw_graph(d, int(w * 0.76), int(h * 0.36), int(h * 0.1))
    img.save(os.path.join(BANNERS, name + ".png"))


def header(name, text):
    w, h = 1600, 92
    img = background(w, h)
    d = ImageDraw.Draw(img)
    d.rectangle([0, h - 6, w, h], fill=GOLD_DARK)
    d.rectangle([0, h - 6, w, h - 5], fill=GOLD)
    d.text((40, 20), text, font=font(46, True), fill=GOLD_LIGHT)
    img.save(os.path.join(BANNERS, name + ".png"))


base = icon32()
for size in (32, 64, 128, 256, 512, 1024):
    base.resize((size, size), Image.NEAREST).save(os.path.join(ICON, "icon_%d.png" % size))
base.resize((512, 512), Image.NEAREST).save(os.path.join(ICON, "barelyquesting_logo.png"))
banner("banner", 1600, 300, "Barely Questing", "Quests as a node graph or a list. Teams, themes, an editor, scripts.")
banner("social", 1280, 640, "Barely Questing", "An in-game quest book for Forge and NeoForge")
banner("features", 1920, 1080, "Barely Questing", "Graph and list views | teams | themes | editor | ModJS and KubeJS | recipe viewer clicks")
for name, text in (("h_features", "Features"), ("h_themes", "Themes"), ("h_editor", "The in-game editor"),
                   ("h_scripting", "Scripting"), ("h_compat", "Compatibility"), ("h_install", "Install"), ("h_faq", "FAQ")):
    header(name, text)
print("branding written")
