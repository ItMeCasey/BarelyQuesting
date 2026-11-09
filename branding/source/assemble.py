"""Builds banners.html (SMWTI's stage, pixel font and helpers plus the Barely Questing assets in banners_tail.js) and
render_banners.py from SMWTI's. Run from the repository root: python branding/source/assemble.py"""
from pathlib import Path

SMWTI = Path(r"C:\Users\casey\Desktop\SMWTI\branding\source")
HERE = Path(__file__).resolve().parent

src = (SMWTI / "banners.html").read_text(encoding="utf8")
head = src[:src.index("// ---------- sprites (original) ----------")]
head = head.replace("Renders the SMWTI banners", "Renders the Barely Questing banners")
css = """
  .qgraph { position: relative; }
  .qnode { position: absolute; width: 44px; height: 44px; border-radius: 8px; border: 3px solid #101015; display: grid; place-items: center; }
  .qline { position: absolute; background: #6a6a80; }
  .qpanel { display: flex; flex-direction: column; gap: 10px; width: 360px; padding: 14px 18px 16px; border-radius: 6px;
    background: rgba(16,14,28,.94); border: 2px solid #3a3360; box-shadow: inset 0 0 0 2px rgba(0,0,0,.6), 0 18px 40px rgba(0,0,0,.45); }
  .qpanel .name { font-size: 26px; font-weight: 600; color: var(--gold-hi); }
  .qpanel .st { font-size: 18px; color: var(--muted); }
  .btn { align-self: flex-start; font-size: 18px; padding: 5px 16px; border-radius: 3px; background: #3a3a48; border: 2px solid #55556a; color: var(--text); }
  .btn.gold { background: var(--gold); border-color: var(--gold-lo); color: var(--bg0); font-weight: 600; }
</style>"""
head = head.replace("</style>", css, 1)
(HERE / "banners.html").write_text(head + (HERE / "banners_tail.js").read_text(encoding="utf8"), encoding="utf8")

r = (SMWTI / "render_banners.py").read_text(encoding="utf8")
r = r.replace("smwti-chrome-", "barelyquesting-chrome-")
a = r.index("ASSETS = {")
b = r.index("}\n\nOUT.mkdir")
new = '''ASSETS = {
    "banner": (800, 150, 2),
    "themes_strip": (800, 150, 2),
    "social": (1280, 640, 1),
    "features": (1920, 1080, 1),
    "themes": (1920, 1080, 1),
    **{f"h_{k}": (800, 46, 2) for k in ("features", "themes", "editor", "scripting", "compat", "install", "faq")},
'''
(HERE / "render_banners.py").write_text(r[:a] + new + r[b:], encoding="utf8")
print("assembled")
