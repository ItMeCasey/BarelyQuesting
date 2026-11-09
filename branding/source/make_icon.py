"""Draws the Barely Questing icon as 32x32 pixel art (a quest scroll with a node path and a gold star) and writes it at several sizes.

Run from the repository root: python branding/source/make_icon.py
Writes branding/icon/icon_<size>.png, icon.svg and barelyquesting_logo.png (for the jar). Then run render_banners.py.
"""
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent.parent / "icon"
NIGHT, NIGHT2, EDGE = (15, 14, 26), (26, 24, 48), (46, 42, 70)
GOLD, GOLD_LIGHT, GOLD_DARK = (242, 179, 61), (255, 226, 140), (178, 116, 32)
BLUE, GREEN = (122, 168, 255), (118, 176, 82)
PAPER, PAPER_DARK, INK = (232, 213, 168), (205, 181, 130), (59, 42, 26)

img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.rectangle([1, 0, 30, 31], fill=EDGE)
d.rectangle([0, 1, 31, 30], fill=EDGE)
d.rectangle([1, 1, 30, 30], fill=NIGHT2)
d.rectangle([2, 2, 29, 29], fill=NIGHT)
d.rectangle([6, 4, 25, 27], fill=INK)
d.rectangle([7, 5, 24, 26], fill=PAPER)
d.rectangle([7, 5, 24, 6], fill=(250, 236, 200))
d.rectangle([7, 25, 24, 26], fill=PAPER_DARK)
d.rectangle([5, 3, 26, 5], fill=INK)
d.rectangle([6, 4, 25, 4], fill=GOLD_LIGHT)
d.rectangle([5, 26, 26, 28], fill=INK)
d.rectangle([6, 27, 25, 27], fill=GOLD_DARK)
d.line([(11, 21), (11, 15)], fill=INK)
d.line([(11, 15), (20, 15)], fill=INK)
d.line([(20, 15), (20, 10)], fill=INK)
for x, y, c in ((11, 21, BLUE), (11, 15, GREEN), (20, 15, GREEN)):
    d.rectangle([x - 2, y - 2, x + 2, y + 2], fill=INK)
    d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=c)
sx, sy = 20, 9
for dx, dy in ((0, -3), (0, 3), (-3, 0), (3, 0)):
    d.point((sx + dx, sy + dy), fill=GOLD_DARK)
d.rectangle([sx - 2, sy - 1, sx + 2, sy + 1], fill=GOLD)
d.rectangle([sx - 1, sy - 2, sx + 1, sy + 2], fill=GOLD)
d.point((sx, sy), fill=GOLD_LIGHT)

OUT.mkdir(parents=True, exist_ok=True)
for size in (32, 64, 128, 256, 512, 1024):
    img.resize((size, size), Image.NEAREST).save(OUT / f"icon_{size}.png")
img.resize((512, 512), Image.NEAREST).save(OUT / "barelyquesting_logo.png")
rects = []
px = img.load()
for y in range(32):
    for x in range(32):
        r, g, b, a = px[x, y]
        if a:
            rects.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="#{r:02x}{g:02x}{b:02x}"/>')
(OUT / "icon.svg").write_text('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" shape-rendering="crispEdges">' + "".join(rects) + "</svg>\n")
print("icon written")
