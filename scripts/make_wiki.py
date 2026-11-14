#!/usr/bin/env python3
"""Writes the Barely Questing wiki (wiki/barelyquesting/) in the itmecasey.com wiki format. Run from the repository root."""
import json
import os
import shutil

OUT = os.path.join("wiki", "barelyquesting")
PAGES = os.path.join(OUT, "pages")
os.makedirs(PAGES, exist_ok=True)
os.makedirs(os.path.join(OUT, "images"), exist_ok=True)
shutil.copy(os.path.join("branding", "icon", "icon_256.png"), os.path.join(OUT, "images", "icon.png"))

INFOBOX = """infobox:
  Loaders: NeoForge, Forge
  Minecraft: 1.20.1, 1.21.1, 26.1.2, 26.2, 26.3
  Side: Both (install on the server and every client)
  Required mods: None
  Optional: ModJS, KubeJS, JEI, REI, EMI, BEI
  License: MIT
"""

pages = {}

pages["home"] = ("Barely Questing", "An in-game quest book for Forge and NeoForge: node graph or list, tasks, rewards, teams, themes, an editor and scripting.", True, """
Barely Questing is a quest book for your world or modpack. Quests sit in chapters; each chapter can be read as a **node graph** with dependency lines or as a **list** with checkboxes. Players claim rewards in the book, play alone or in a team, and pick a look from the resource-pack themes. Pack makers edit the book in game, import an FTB Quests or Better Questing book, or script quests with ModJS and KubeJS.

## Start here

- [[install]] to put it in a game.
- [[first-steps]] for the book, the J key and your first quest.
- [[quests]] for how quests, tasks, rewards and locks fit together.
- [[editor]] to build your own book.
- [[scripting]] for ModJS and KubeJS.

> [!NOTE]
> The mod adds no blocks and no machines. It is a screen, a set of files and a server-side rule engine.
""")

pages["install"] = ("Install and versions", "Which jar to pick and what it needs.", False, """
Barely Questing ships one jar per loader and Minecraft version: Forge and NeoForge for 1.20.1, 1.21.1, 26.1.2, 26.2 and 26.3, named `barelyquesting-<loader>-mc<version>-<mod version>.jar`. Put it in `mods/` on the server and on every client. No other mod is needed.

| Minecraft | Loaders | Java |
|---|---|---|
| 1.20.1 | Forge, NeoForge | 17 |
| 1.21.1 | Forge, NeoForge | 21 |
| 26.1.2, 26.2, 26.3 | Forge, NeoForge | 25 |

Optional mods: [[scripting]] (ModJS, KubeJS) and [[recipe-viewers]] (JEI, REI, EMI, BEI). See [[versions]] for what was checked on which build.
""")

pages["first-steps"] = ("First steps", "Open the book and finish your first quest.", False, """
1. Press **J** (rebind it under Controls, Barely Questing) or run `/barelyquesting open`.
2. Pick a chapter on the left. The middle shows the quests as a graph (drag to pan, scroll to zoom) or, with the **List** button, as a list grouped by status.
3. Click a quest. The panel on the right shows its description, tasks and rewards.
4. Do the tasks. Some count by themselves (kill, craft, obtain, visiting places). **Submit** tasks have a button to hand items in; **checkmark** tasks have a **Check** button.
5. When a quest is complete its rewards appear with **Claim** buttons, or **Claim all**.
6. The star button pins a quest to the HUD tracker in the corner of the game screen.

The first time the mod runs it writes a small starter book to `config/barelyquesting/` so there is something to look at and edit.
""")

pages["quests"] = ("Quests, tasks, rewards and locks", "How a quest is built and what each task, reward and lock type does.", False, """
A **quest** has a title, a description, an icon, a position on the chapter's grid, dependencies, tasks, rewards and locks. A quest is complete when all its tasks are done (or any one, with **Any task**), and it opens when its dependencies are complete and no lock is closed. **Optional** quests never block others; **Hidden** quests stay out of sight until their dependencies are done.

## Tasks

| Type | What counts |
|---|---|
| `item` | Hold the items (counted from the inventory), or with `consume` hand them in with the Submit button. Matches ids, tags (`#minecraft:logs`) and, if asked, item data. |
| `craft` | Crafting or smelting the item. |
| `obtain` | Picking up or crafting the item, even if you give it away later. |
| `kill` | Kill an entity type or tag, `count` times. |
| `advancement` | Earn an advancement. |
| `statistic` | A game statistic (`custom`, `mined`, `crafted`, `used`, `broken`, `picked_up`, `dropped`, `killed`, `killed_by`) reaches a value. |
| `location` | Be within `radius` blocks of x y z, optionally in one dimension. |
| `dimension`, `biome`, `structure` | Be in a dimension, a biome (or biome tag), or inside a structure. |
| `checkmark` | The player ticks it off. |
| `scripted` | Decided by a script, see [[scripting]]. |

Progress never goes down, so a quest stays done.

## Rewards

| Type | Gives |
|---|---|
| `item` | Items (what does not fit drops at the player's feet). |
| `xp` | Experience `points` or `levels`. |
| `command` | Runs a command as the server with the player as `@s`; `{player}` is the name. |
| `scripted` | Whatever the script does. |

Each reward has a **scope**: `player` (every team member can claim it) or `team` (claimed once for the whole team). `auto: true` claims it the moment the quest completes.

## Locks

`quest` (another quest must be done), `time` (open from and/or until a date) and `script` (a script condition).

## Files

```json
{
  "id": "wood",
  "title": "Gather Wood",
  "description": "Punch a tree and **collect 4 logs**.",
  "icon": "minecraft:oak_log",
  "x": 0, "y": 0,
  "dependencies": [],
  "tasks": [{ "id": "t0", "type": "obtain", "item": { "tag": "minecraft:logs", "count": 4 } }],
  "rewards": [{ "id": "r0", "type": "item", "item": { "item": "minecraft:apple", "count": 8 } }]
}
```

Descriptions use a small markup: `**bold**`, `*italic*`, `__underline__`, `~~strike~~`, `&c` color codes (`&r` resets) and `[label](https://link)`.
""")

pages["teams"] = ("Teams", "Share progress with other players.", False, """
Everyone starts alone with their own progress. Open **Team** in the book to create a team, invite a player by name, accept an invitation, leave, kick a member or disband the team. Members share task progress and completed quests, so one player's work counts for all. Rewards with the `team` scope are claimed once; `player` rewards can be claimed by each member.

Leaving a team returns you to your own progress, and the team's progress stays with the team. The server owns teams and progress, so they cannot be changed from a client.
""")

pages["themes"] = ("Themes", "Change the look of the book with a resource pack.", False, """
Open **Themes** in the book to choose one. **Default** is a dark panel style drawn with plain fills; **Parchment** is warm paper drawn with sprites. Add more with a resource pack and press F3+T.

## Making a theme

`assets/<namespace>/barelyquesting/themes/index.json` lists your theme ids and each id has a file next to it:

```json
{ "themes": ["mytheme"] }
```

```json
{
  "name": "My Theme", "author": "You", "font": "", "node_size": 26,
  "colors": { "panel": "#2b2b33", "text": "#ededf5", "node_complete": "#4fc36b" },
  "sprites": {
    "panel": { "texture": "mymod:textures/gui/panel.png", "size": [32, 32], "border": [6, 6, 6, 6] }
  },
  "sounds": { "click": "minecraft:ui.button.click", "complete": "minecraft:entity.player.levelup" }
}
```

Anything you leave out falls back to the built-in values, so a theme can be a few colors. Colors are `#RRGGBB` or `#AARRGGBB`. Sprites are a texture, its size, an optional `region` and nine-slice `border` values (left, top, right, bottom).

Color names: `background`, `panel`, `panel_edge`, `panel_inset`, `header`, `text`, `text_muted`, `title`, `accent`, `button`, `button_hover`, `button_off`, `tab`, `tab_selected`, `slot`, `bar`, `bar_fill`, `line`, `line_done`, `node_locked`, `node_available`, `node_progress`, `node_complete`, `node_claimed`, `node_selected`, `danger`, `success`.

Sprite names: `panel`, `panel_inset`, `header`, `button`, `button_hover`, `button_off`, `tab`, `tab_selected`, `slot`, `bar`, `node_locked`, `node_available`, `node_progress`, `node_complete`, `node_claimed`.

Sound keys: `click`, `complete`, `claim`, `open`.
""")

pages["editor"] = ("The in-game editor", "Build and change the book without leaving the game.", False, """
The **Edit** button appears for operators, players in creative mode (unless turned off) and names on the editor list (`/barelyquesting editor add <name>`). `/barelyquesting edit` opens the book in edit mode. A pack that ships a finished questline can turn editing off for everyone; see [[settings]].

- **Move** a quest by dragging its node; it snaps to the grid.
- **Add** a quest with **+ Quest** or by right-clicking an empty spot. **+ Chapter** adds a chapter; click the selected chapter again to rename it, set its icon and group, reorder it or delete it.
- **Link**: select a quest, press **Link**, click the quest that should depend on it. Click again to remove the link. Loops are refused.
- **Fields**: select a quest to edit its title, description, icon, grid position, Optional, Hidden and Any task flags, dependencies, tasks, rewards and locks. Each task, reward and lock has its type and its own fields; items open a searchable picker (items or tags, with a count).
- **Undo and redo** with the buttons or Ctrl+Z and Ctrl+Y. Deleting a quest or chapter can be undone.
- The eye button in the panel previews a quest the way a player sees it.

Every change goes to the server, which saves the files and shows it to everyone online at once. Text fields are sent when you press Enter or click away.
""")

pages["settings"] = ("Config files", "The .toml settings, the in-game config screen and migrating from older versions.", False, """
The mod's settings are standard Forge and NeoForge config files, written to `config/` the first time the game starts, with a comment above every value.

| File | Side | Keys |
|---|---|---|
| `barelyquesting-common.toml` (`barelyquesting-startup.toml` on NeoForge 26.3) | server, and singleplayer worlds | `editing`, `creative_edits`, `editors`, `give_book_on_join`, `open_book_on_join` |
| `barelyquesting-client.toml` | each player's own game | `hud_tracker`, `theme`, `view`, `last_chapter`, `pinned_quests` |

On NeoForge the values also appear in the in-game config screen (Mods, Barely Questing, Config). Forge has no config screen; edit the file.

| Key | Values | Default | What it does |
|---|---|---|---|
| `editing` | `true`, `false` | `true` | `false` hides the **Edit** button and refuses every edit, `/barelyquesting edit` and the editor packets, for everyone including operators. Use it for a finished questline. Scripts, `/barelyquesting import` and `/barelyquesting reload` keep working, so the pack author still changes the book by editing the quest files and reloading. |
| `creative_edits` | `true`, `false` | `true` | Whether players in creative mode may edit (when `editing` is on). |
| `editors` | list of names | empty | Players allowed to edit besides operators. `/barelyquesting editor add` and `remove` change this list. |
| `give_book_on_join` | `true`, `false` | `false` | Gives each player one **Quest Book** the first time they join the world; it is dropped at their feet if the inventory is full. A rejoin or a death never repeats it. |
| `open_book_on_join` | `true`, `false` | `false` | Opens the book on a player's first join to the world. |
| `hud_tracker` | `true`, `false` | `true` | Shows the pinned-quest tracker on the game screen. |
| `theme` | theme id | empty | The book theme, for example `barelyquesting:parchment`. Empty is the default theme. |
| `view` | `graph`, `list` | `graph` | How a chapter is shown. |
| `last_chapter` | chapter id | empty | The chapter the book reopens on. |
| `pinned_quests` | list of quest ids | empty | Quests pinned to the tracker. |

The quest book itself (title, chapter order and every quest) is data, not settings: it stays in `config/barelyquesting/book.json` and `chapters/*.json`. The first-join list is kept per world in `barelyquesting/welcomed.json` inside the world folder; delete a player's entry to welcome them again.

## Migrating

Before version 2026.11.9 the settings lived in `config/barelyquesting/book.json` (the first five keys above) and `config/barelyquesting/client.json` (the rest). The first time 2026.11.9 or later starts and a new toml file does not exist yet, every value you had set is copied into it. Then the old file is kept as a backup next to itself: `client.json` becomes `client.json.back`, and `book.json` (which still holds the book's title and chapters) is saved as `book.json.back` and rewritten without the setting keys. The first line of a `.back` file says where and why it migrated; the rest is the original file. A `.back` file is never overwritten (`.back2`, `.back3` are used instead). If an old file is corrupt or only partly readable it is left exactly as it is and the unreadable values use their defaults. The migration runs only when the toml does not exist, so it never changes a toml you edited. To go back to an older version, put the `.back` file's content (without its first line) back in place of the file.
""")

pages["commands"] = ("Commands", "The /barelyquesting commands.", False, """
`/barelyquesting` can be shortened to `/bq`.

| Command | Who | What |
|---|---|---|
| `open [quest]` | everyone | Opens the book, optionally on a quest id. |
| `edit` | operators and editors | Opens the book in edit mode. |
| `reload` | operators | Re-reads the quest files and sends them to everyone. Use it after editing the JSON by hand. |
| `complete <player> <quest>` | operators | Completes a quest for the player's team. |
| `reset <player> [quest]` | operators | Clears a quest (or everything) for the player's team. |
| `import <path>` | operators | Converts an FTB Quests or Better Questing book, see [[importing]]. |
| `editor add\\|remove <name>` | operators | Manages the editor list. |
""")

pages["importing"] = ("Importing FTB Quests and Better Questing", "Convert an existing book once.", False, """
Run `/barelyquesting import <path>` with the path to:

- an **FTB Quests** `quests` folder (the one with `chapters/*.snbt`; its `lang/en_us.snbt` is used for titles and descriptions) or a single chapter `.snbt` file, or
- a **Better Questing** `DefaultQuests` folder (`QuestDatabase.json`, `QuestLineDatabase.json`).

Relative paths start in the server folder. The result is added as new chapters in one step, so a single **Undo** takes it back, and it is saved as normal chapter files. Quest ids are made new, dependencies between quests (also across chapters) are kept, and quest positions are mapped to the grid.

What carries over: item, kill, advancement, location, dimension, biome, structure, statistic and checkmark tasks; item, XP and command rewards; titles, descriptions, icons. Anything without an equivalent (loot or choice rewards, observation tasks, custom tasks) is skipped or becomes a checkmark task, and the count is shown in the log. There is no runtime link to either mod.
""")

pages["scripting"] = ("Scripting with ModJS and KubeJS", "Create quests, add types and listen to events from scripts.", False, """
With ModJS or KubeJS installed, scripts get a `BarelyQuesting` object. Neither mod is required.

## ModJS

The global `BarelyQuesting` is available in startup and server scripts; client scripts do not get it, since quests run on the server. Quests made in startup scripts are queued until the world loads.

## KubeJS

Barely Questing writes `kubejs/startup_scripts/barelyquesting.js` once, which sets `global.BarelyQuesting`. Use `global.BarelyQuesting` in your scripts. Restart after changing callback registrations; they are not cleaned up on a script reload.

## What you can do

```js
const BQ = global.BarelyQuesting   // KubeJS; in ModJS use BarelyQuesting directly

BQ.chapter('my_pack', 'My Pack', 'minecraft:diamond')
BQ.quest('my_pack', JSON.stringify({
  id: 'sky', title: 'Touch the sky',
  tasks: [{ id: 't', type: 'scripted', name: 'high', count: 1 }],
  rewards: [{ id: 'r', type: 'scripted', name: 'gift' }]
}))

// a scripted task: return the progress to set (higher than now to advance)
BQ.registerTask('high', ctx => ctx.y() > 200 ? ctx.target() : ctx.current())
// a scripted reward
BQ.registerReward('gift', ctx => { ctx.giveItem('2 minecraft:diamond'); ctx.addLevels(1) })
// a lock: return true to keep the quest closed
BQ.registerLock('night_only', ctx => false)
// events
BQ.onQuestCompleted(e => console.log(e.questId() + ' done for team ' + e.teamId()))
```

| Call | Does |
|---|---|
| `chapter(id, title, icon)` | Adds or updates a chapter. |
| `quest(chapterId, json)` | Adds or replaces a quest from JSON in the file format. Give it an `id` so it is replaced, not duplicated, each start. Returns the id. |
| `removeQuest(id)`, `removeChapter(id)` | Removes them. |
| `registerTask(name, fn)` | Polled scripted task (`{"type":"scripted","name":name}`). |
| `registerTaskEvents(name, fn)` | Event-driven scripted task; `fn` gets `kind()` (`kill`, `craft`, `pickup`, `advancement`), `key()`, `amount()` and returns the progress to add. |
| `newTaskType(type, defaultTarget, fn)`, `newRewardType(type, fn)` | A whole new type usable as `{"type": ...}`. |
| `registerReward(name, fn)`, `registerLock(name, fn)` | Scripted reward and lock. |
| `onQuestStarted`, `onTaskProgressed`, `onQuestCompleted`, `onQuestClaimed` | Events with `questId()`, `questTitle()`, `teamId()`, `taskId()`, `playerId()`. |
| `complete(playerUuid, questId)`, `reset(playerUuid, questId)`, `addProgress(playerUuid, questId, taskId, n)` | Change progress for the player's team. |
| `isComplete(playerUuid, questId)`, `taskProgress(...)`, `teamName(playerUuid)`, `bookJson()` | Read state. |

Contexts: task functions get `player` helpers (`playerId()`, `playerName()`, `x() y() z()`, `dimension()`, `biome()`, `count(itemSpec)`, `hasAdvancement(id)`, `stat(type, key)`), parameters (`param(key, default)`, `number(key, default)`) and `current()` and `target()`. Reward functions get `giveItem`, `addXp`, `addLevels`, `command` and `message`.

Changes made by scripts go through the same path as the editor, so they are saved and synced.
""")

pages["recipe-viewers"] = ("Recipe viewers", "Click a quest item to open its recipes.", False, """
In the quest panel, **left click** an item to open its recipes and **right click** to open where it is used, in the recipe viewer you have installed: JEI, REI, EMI or BEI (tried in that order). With none installed nothing happens. Tags show a rotating member and open that item's page.

See [[versions]] for which viewers were checked on which build.
""")

pages["versions"] = ("Versions and known gaps", "What was checked on each build, and what was not.", False, """
All ten builds compile, run the shared unit tests and pass a headless dedicated-server self test (a real player, item and tag matching, the engine, saving, commands). The screen, themes, editor, toasts, HUD, viewers and scripting bridges were run on a real client on **NeoForge 26.1.2** only. The other nine builds are **not yet seen in a real client**.

| Build | Server self test | Real client |
|---|---|---|
| NeoForge 26.1.2 | pass | yes |
| Forge 26.1.2, NeoForge and Forge 26.2, 26.3 | pass | not yet |
| NeoForge and Forge 1.21.1 | pass | not yet |
| NeoForge and Forge 1.20.1 | pass | not yet |

## Known gaps

- No craftable Quest Book item yet; use J or `/barelyquesting open`.
- No viewer button that opens the quest for an item.
- The editor's item picker lists registry items; it is not driven by JEI or BEI search.
- KubeJS support is a helper script, not a native plugin, and KubeJS has no build for 26.2, 26.3, or Forge 1.21.1 and 26.x. EMI has no build for 26.1.2 to test with.
- BEI lookups are wired on the 26.x builds only.
- Multiplayer with two real clients has not been run; the server and wire format are unit tested.
""")

pages["troubleshooting"] = ("Troubleshooting", "When something does not work.", False, """
**J does nothing.** Check Controls, Barely Questing. The mod must be on the server too; the book stays empty if the server does not have it.

**The book is empty or old.** Run `/barelyquesting reload` after editing the JSON files by hand. Files that are not valid JSON are skipped and named in the server log.

**A task does not count.** Only open quests count. Check that every dependency is complete and no lock is closed (the panel names what is missing). Item tasks match by id; use a tag (`#minecraft:logs`) for variants, and turn item data matching on only when you need it.

**I cannot see Edit.** You need to be an operator, in creative mode, or on the editor list (`/barelyquesting editor add <name>`).

**I lost a quest.** Use Undo in the editor right away, or restore `config/barelyquesting/chapters/<chapter>.json` from a backup; the files are plain JSON.

**A theme is missing.** The pack needs `assets/<namespace>/barelyquesting/themes/index.json` listing the id; press F3+T.

**Bugs and crashes:** [github.com/ItMeCasey/BarelyQuesting/issues](https://github.com/ItMeCasey/BarelyQuesting/issues) (include the mod version, loader and Minecraft version).
""")

nav = [
    ("Getting started", ["home", "install", "first-steps"]),
    ("Using the book", ["quests", "teams", "themes", "recipe-viewers"]),
    ("Pack makers", ["editor", "settings", "commands", "importing", "scripting"]),
    ("Reference", ["versions", "troubleshooting"]),
]
for slug, (title, summary, home, body) in pages.items():
    with open(os.path.join(PAGES, slug + ".md"), "w", encoding="utf8", newline="\n") as f:
        f.write("---\ntitle: %s\nsummary: %s\n" % (title, summary))
        if home:
            f.write("image: images/icon.png\n" + INFOBOX)
        f.write("---\n" + body.lstrip("\n"))

mod = {
    "name": "Barely Questing",
    "tagline": "An in-game quest book for Forge and NeoForge: node graph or list, tasks, rewards, teams, themes, an editor and ModJS and KubeJS scripting.",
    "icon": "images/icon.png",
    "home": "home",
    "minecraft": "1.20.1, 1.21.1, 26.1.2, 26.2, 26.3",
    "loaders": "NeoForge, Forge",
    "license": "Barely Questing is released under the MIT License.",
    "links": [
        {"label": "Source", "url": "https://github.com/ItMeCasey/BarelyQuesting"},
        {"label": "Issues", "url": "https://github.com/ItMeCasey/BarelyQuesting/issues"},
    ],
}
mod["nav"] = [{"title": t, "pages": [{"slug": s, "title": pages[s][0]} for s in ss]} for t, ss in nav]
with open(os.path.join(OUT, "mod.json"), "w", encoding="utf8", newline="\n") as f:
    json.dump(mod, f, indent=2)
    f.write("\n")
src = os.path.join(os.environ.get("SITE_WIKI", ""), "barelypipes", "index.html")
if os.path.exists(src):
    html = open(src, encoding="utf8").read()
    html = html.replace("Barely Pipes", "Barely Questing").replace("barelypipes", "barelyquesting")
    html = html.replace("seven pipes in one block, with tiers, per-face modes and upgrades, for Forge and NeoForge", "an in-game quest book with a node graph, list view, teams, themes, an editor and scripting, for Forge and NeoForge")
    open(os.path.join(OUT, "index.html"), "w", encoding="utf8", newline="\n").write(html)
print("wiki written:", len(pages), "pages")
