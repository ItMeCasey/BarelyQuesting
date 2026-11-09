// ---------- sprites (original) ----------
const SPR = {
  scroll: {pal:{k:"#3b2a1a",p:"#e8d5a8",P:"#cdb582",g:"#f2b33d",G:"#ffe28c",b:"#7aa8ff",n:"#76b052"}, rows:[
    "..GGGGGGGG..",".kkkkkkkkkk.","kpppppppppPk","kpnnpppppPPk","kpnnpppppPPk","kpkpppgggPPk","kpkpppgGgPPk",
    "kpkkkpgggPPk","kpppnnpppPPk","kpppnnpppPPk","kpppppppPPPk",".kkkkkkkkkk."]},
  star: {pal:{g:"#f2b33d",G:"#ffe28c",d:"#b27420"}, rows:[
    "....g....","....g....","..ggGgg..","ggggGgggg",".ggGGGgg.","..ggggg..",".gg.g.gg.","g.......g"]},
  check: {pal:{n:"#ffffff",k:"#1c5a2e"}, rows:[
    "......nn",".....nnk","n...nnk.","nn.nnk..",".nnnk...","..nk...."]},
  log: {pal:{b:"#a8743a",d:"#7a5126",k:"#2a1a0e",l:"#d9a85a"}, rows:[
    "kkkkkkkk","kbbllbbk","kbllllbk","kbllllbk","kbbllbbk","kddddddk","kkkkkkkk"]},
  sword: {pal:{i:"#cfd6e6",I:"#8e96aa",h:"#8a5a32",g:"#f2b33d"}, rows:[
    "......ii",".....iIi","....iIi.","...iIi..","g.iIi...",".gIi....","..gh....",".h.g....","h......."]},
};
function sprite(name, s) {
  const {pal, rows} = SPR[name];
  const c = document.createElement("canvas");
  c.width = rows[0].length * s; c.height = rows.length * s;
  const x = c.getContext("2d");
  rows.forEach((r, y) => [...r].forEach((p, i) => { if (pal[p]) { x.fillStyle = pal[p]; x.fillRect(i * s, y * s, s, s); } }));
  return c;
}

// ---------- helpers ----------
function half(c) { c.style.width = c.width / 2 + "px"; c.style.height = c.height / 2 + "px"; return c; }
function el(tag, attrs = {}, ...kids) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) k === "style" ? (e.style.cssText = v) : e.setAttribute(k, v);
  for (const k of kids) e.append(k);
  return e;
}
function iconImg(px) { return el("img", {class: "icon", src: "../icon/icon_512.png", width: px, height: px}); }
function bar(pct, color, label) {
  return el("div", {class: "bar"}, el("i", {style: `width:${pct}%;background:${color}`}), el("span", {}, label));
}
const COL = {done: "#4fc36b", prog: "#c9a227", open: "#4a6fa5", lock: "#3a3a44"};
// A node graph like the book's graph view. nodes: [cellX, cellY, state, sprite]; lines: [from, to]
function graph(nodes, lines, cell = 64) {
  const minX = Math.min(...nodes.map(n => n[0])), minY = Math.min(...nodes.map(n => n[1]));
  const maxX = Math.max(...nodes.map(n => n[0])), maxY = Math.max(...nodes.map(n => n[1]));
  const g = el("div", {class: "qgraph", style: `width:${(maxX - minX) * cell + 60}px;height:${(maxY - minY) * cell + 60}px`});
  const pos = nodes.map(n => [(n[0] - minX) * cell + 8, (n[1] - minY) * cell + 8]);
  for (const [a, b] of lines) {
    const ax = pos[a][0] + 22, ay = pos[a][1] + 22, bx = pos[b][0] + 22, by = pos[b][1] + 22, mx = Math.round((ax + bx) / 2);
    const done = nodes[a][2] === "done" ? "background:#4fc36b" : "";
    g.append(el("div", {class: "qline", style: `left:${Math.min(ax, mx)}px;top:${ay - 1}px;width:${Math.abs(mx - ax)}px;height:3px;${done}`}));
    g.append(el("div", {class: "qline", style: `left:${mx - 1}px;top:${Math.min(ay, by)}px;width:3px;height:${Math.abs(by - ay) + 3}px;${done}`}));
    g.append(el("div", {class: "qline", style: `left:${Math.min(mx, bx)}px;top:${by - 1}px;width:${Math.abs(bx - mx)}px;height:3px;${done}`}));
  }
  nodes.forEach((n, i) => {
    const d = el("div", {class: "qnode", style: `left:${pos[i][0]}px;top:${pos[i][1]}px;background:${COL[n[2]]}`});
    d.append(sprite(n[3], 3));
    g.append(d);
  });
  return g;
}
const DEMO = [[0, 1, "done", "log"], [1, 1, "done", "log"], [2, 1, "prog", "sword"], [3, 0, "open", "star"], [3, 2, "open", "check"], [4, 1, "lock", "scroll"]];
const DEMO_LINES = [[0, 1], [1, 2], [2, 3], [2, 4], [3, 5], [4, 5]];
function questPanel() {
  return el("div", {class: "qpanel"},
    el("div", {class: "row"}, sprite("sword", 4), el("div", {}, el("div", {class: "name"}, "Night Watch"), el("div", {class: "st"}, "In progress"))),
    el("div", {class: "line"}, "Defeat 3 zombies after dark."),
    bar(66, "linear-gradient(90deg,#3fa060,#7ed957)", "Kill Zombie  2 / 3"),
    el("div", {class: "line"}, el("span", {class: "k"}, "Reward  "), "100 XP, Stone Pickaxe"),
    el("div", {class: "btn gold"}, "Claim all"));
}

// ---------- themes ----------
const THEMES = [
  ["Default", {p: "#2b2b33", e: "#55556a", t: "#ededf5", s: "#a0a0b4", b: "#15151b", a: "#f2b33d"}, "edge shadow accent", true],
  ["Parchment", {p: "#e8d5a8", e: "#8b6b3d", t: "#3b2a1a", s: "#7a6444", b: "#604828", a: "#9c5a14"}, "edge accent"],
];
function themePanel([label, c, flags]) {
  const vars = Object.entries(c).map(([k, v]) => `--${k}:${v}`).join(";");
  return el("div", {class: "tpanel " + flags, style: vars},
    el("div", {class: "row"}, sprite("sword", 4), el("div", {}, el("div", {class: "name"}, "Night Watch"), el("div", {class: "tmod"}, "In progress"))),
    bar(66, "linear-gradient(90deg,#3fa060,#7ed957)", "Kill Zombie  2 / 3"),
    el("div", {class: "line"}, "Reward: 100 XP"));
}

// ---------- assets ----------
const ASSETS = {
  banner: [800, 150, (r) => {
    r.style.cssText += "display:flex;align-items:center;gap:24px;padding:0 28px";
    r.append(iconImg(104));
    r.append(el("div", {style: "display:flex;flex-direction:column;align-items:flex-start;gap:11px;flex:1"},
      half(pixelText("Barely Questing", 7)),
      el("div", {class: "tag", style: "font-size:16px"}, "A quest book in your game. ", el("b", {}, "Graph, list, teams, themes.")),
      el("div", {class: "meta", style: "gap:6px"},
        el("span", {class: "pill on", style: "font-size:11px;padding:3px 9px"}, "Forge + NeoForge"),
        el("span", {class: "pill", style: "font-size:11px;padding:3px 9px"}, "In-game editor"),
        el("span", {class: "pill", style: "font-size:11px;padding:3px 9px"}, "ModJS + KubeJS"))));
    const g = graph(DEMO, DEMO_LINES, 52); g.style.cssText += ";zoom:.5;transform:rotate(-2deg)"; r.append(g);
  }],
  themes_strip: [800, 150, (r) => {
    r.style.cssText += "display:flex;align-items:center;justify-content:center;gap:30px";
    for (const t of THEMES) {
      const p = themePanel(t); p.style.cssText += ";zoom:.5";
      r.append(el("div", {}, p, el("div", {class: "tlabel", style: "margin-top:7px"}, half(pixelText(t[0], 4, t[3] ? BEVEL : WHITE)))));
    }
  }],
  social: [1280, 640, (r) => {
    r.style.cssText += "display:flex;flex-direction:column;align-items:center;justify-content:center;gap:28px;text-align:center";
    r.append(iconImg(220));
    r.append(pixelText("Barely Questing", 7));
    r.append(el("div", {class: "tag"}, "An in-game quest book for Forge and NeoForge."));
  }],
  themes: [1920, 1080, (r) => {
    r.style.cssText += "display:flex;flex-direction:column;align-items:center;justify-content:center;gap:60px";
    r.append(el("div", {style: "display:flex;align-items:center;gap:36px"}, iconImg(130),
      el("div", {style: "display:flex;flex-direction:column;gap:16px"}, pixelText("Resource pack themes", 7),
        el("div", {class: "tag"}, "Colors, panels, node sprites, font and sounds. F3+T reloads"))));
    const row = el("div", {style: "display:flex;gap:80px;zoom:1.5"});
    for (const t of THEMES) row.append(el("div", {}, themePanel(t), el("div", {class: "tlabel"}, pixelText(t[0], 4, t[3] ? BEVEL : WHITE))));
    r.append(row);
  }],
  features: [1920, 1080, (r) => {
    r.style.cssText += "display:flex;flex-direction:column;align-items:center;justify-content:center;padding:40px 90px;gap:60px";
    r.append(el("div", {style: "display:flex;align-items:center;gap:36px"}, iconImg(140),
      el("div", {style: "display:flex;flex-direction:column;gap:16px"}, pixelText("Barely Questing", 7),
        el("div", {class: "tag"}, "Chapters of quests, as a node graph or a list"))));
    const row = el("div", {style: "display:flex;gap:90px;align-items:center"});
    const g = graph(DEMO, DEMO_LINES, 84); g.style.cssText += ";zoom:1.25"; row.append(g);
    const q = questPanel(); q.style.cssText += ";zoom:1.35"; row.append(q);
    r.append(row);
    r.append(el("div", {class: "meta"}, ...["Item, kill, craft, location tasks", "Teams", "Themes", "In-game editor", "ModJS and KubeJS", "Recipe viewer clicks", "MIT license"]
      .map((t, i) => el("span", {class: "pill" + (i === 0 ? " on" : ""), style: "font-size:24px"}, t))));
  }],
};
for (const [k, label] of Object.entries({features: "Features", themes: "Themes", editor: "Editor", scripting: "Scripting", compat: "Compatibility", install: "Install", faq: "FAQ"})) {
  ASSETS["h_" + k] = [800, 46, (r) => {
    r.style.cssText += "display:flex;align-items:center;gap:14px;padding:0 16px;border-top:2px solid var(--gold)";
    r.append(iconImg(30));
    r.append(half(pixelText(label, 5)));
  }];
}

const name = location.hash.slice(1) || "banner";
const [w, h, build] = ASSETS[name];
const root = document.getElementById("root");
root.className = "stage";
root.style.cssText = `width:${w}px;height:${h}px`;
build(root);
window.ASSET_NAMES = Object.keys(ASSETS);
</script>
</body>
</html>
