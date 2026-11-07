# Barely Questing plan

An in-game questing guide for Minecraft in the Barely family. The decided spec is [DESIGN_BRIEF.md](DESIGN_BRIEF.md); this file is how it is built.

## Decisions

- **Name and id:** Barely Questing, mod id `barelyquesting`, group `com.itmecasey.barelyquesting`, repo `ItMeCasey/BarelyQuesting`, wiki slug `barelyquesting`.
- **One sentence:** An in-game questing guide with a node-graph and a list view, tasks and rewards, teams, resource-pack themes, an in-game editor and ModJS/KubeJS scripting.
- **Side:** both. The server owns quests, teams and progress; the client draws the book.
- **License:** MIT. **Version:** `YYYY.M.n`, first release `2026.11.1`.
- **Targets:** Forge and NeoForge on 1.20.1 (Java 17), 1.21.1 (Java 21), 26.1.2, 26.2, 26.3 (Java 25): ten modules, ten jars
  `barelyquesting-<loader>-mc<version>-<mod version>.jar`.
- **Reference build:** NeoForge 26.1.2. Everything is built and checked there first, then ported.
- **Art:** original. The default theme uses plain fills plus a few generated sprites; no Minecraft textures are shipped.
- **No Modrinth.** Dependencies from our own mods come from the private Maven (`com.itmecasey:bei-api:...`), never from copied jars.

## Layout: how the ten builds share code

```
common/src/main/java        MC-free code (JDK + Gson only): model, JSON store, engine, teams, tasks, rewards, wire format,
                            editor operations, importers, scripting API, theme model, the whole UI toolkit and screens
common/src/main/resources   lang, default themes (JSON + generated sprites), pack metadata
common/src/test/java        JUnit 5 tests for all of it
version-<mc>/src/main/java  the Minecraft calls that differ by version (one folder per Minecraft version, used by both loaders)
version-<mc>/src/dev/java   self test that needs a running server (only with -PdevTools)
forge-<mc>/ neoforge-<mc>/  Gradle build, mod entry, loader events, key registration, network registration, metadata
```

1.20.1 NeoForge compiles the Forge 1.20.1 loader sources (NeoForge 1.20.1 keeps the `net.minecraftforge` packages), as in BarelySorts.
The 26.1.2, 26.2 and 26.3 version folders differ in a handful of calls (mouse and key events, key input constants, `placeItemBackInInventory`).

**Rule:** if code can be written against our own interfaces it goes in `common/`. Minecraft types appear only in
`version-<mc>` (as `Compat.Api`, `GfxImpl`, `QuestScreen`, `McPlayer`) and in the loader modules (events, network, keys).

## Data model (`core.model`)

Quest definitions are server data, edited by ops, and stored as files:

- `Book` (title, default theme hint, chapter order) -> `Chapter` (id, title, icon `ItemSpec`, group, quests) -> `Quest`
  (id, title, rich-text description, icon, x/y on the graph grid, `dependencies` (quest ids), `tasks`, `rewards`, `locks`, `optional`, `hidden until available`).
- `TaskDef`, `RewardDef`, `LockDef` = `{id, type, params: JsonObject}`. Behavior lives in registries of `TaskType`, `RewardType`, `LockType`, so scripts can add types.
- `ItemSpec` = item id, count, optional tag (`#ns:tag`), optional NBT/component data as SNBT string, `matchData` flag.
- IDs are short random strings (stable across renames), `title` is free text.

Task types: `item` (submit or have), `craft`, `obtain`, `kill`, `advancement`, `statistic`, `location`, `dimension`, `biome`, `structure`, `checkmark`, `scripted`.
Reward types: `item`, `xp`, `command`, `scripted`. Lock types: `quest`, `time`, `script`.

## Storage (`core.store`)

```
config/barelyquesting/book.json                 metadata: title, chapter order, format version, settings
config/barelyquesting/chapters/<chapter>.json   one chapter with its quests
<world>/barelyquesting/progress.json            task progress per team, claimed rewards per player
<world>/barelyquesting/teams.json               teams and members
```

Files are pretty-printed with sorted stable key order (diff friendly), hand-editable, written atomically (temp file and move).
A file watcher is not used: `/barelyquesting reload` re-reads the files; the in-game editor writes them itself.

## Engine, teams, progress (`core.progress`, `core.team`)

`QuestEngine` (one per server) owns the `Book`, `TeamManager`, and `Progress`.

- Progress is per team: `taskProgress[team][quest][task] = long`, `completed[team][quest]`, `claimed[quest][reward][player]`.
  Solo players get an automatic one-member team, so the same code runs for solo and shared play. Team and player reward split: a reward has `scope: team|player`.
- Quest status = `LOCKED | AVAILABLE | IN_PROGRESS | COMPLETED | CLAIMED`, computed from dependencies, locks and progress.
- Task evaluation is event based plus a low-rate poll (every 20 ticks) for `item`/`obtain`/`location`/`dimension`/`biome`/`structure`, so it stays cheap.
  Event hooks (kill, craft, advancement, pickup) are called by the loader modules into `QuestEvents`.
- The engine never touches Minecraft: it asks a `PlayerView` (count/take/give items, XP, commands, position, biome, structure, advancements, stats).

## Sync and networking (`core.net`)

One custom payload channel per loader with a single message `Blob(byte[])`. The first byte is the message id; the rest is built by `Wire`
(UTF strings, varints, JSON). This keeps each loader's network code to ~40 lines and identical across versions.

- Server -> client: `BookSync` (gzip JSON of the whole book, chunked), `ProgressSync` (the player's team progress, full then delta), `TeamSync`, `Toast`, `OpenBook`, `EditResult`.
- Client -> server: `Hello` (request sync), `Claim(quest, reward)`, `CheckSubmit(quest, task)` (manual checkmark / submit items), `EditOp(json)`, `Pin(quest)`, `TeamAction`.
- All edits go through `EditOp` and are validated against permissions on the server (op level 2, creative, or a `barelyquesting.edit` flag in `book.json`'s editor list). The server applies the op, saves files, and broadcasts a patch to everyone online.
- Server is authoritative: the client never changes quest data or progress.

## Client UI toolkit (`ui`)

A small retained-mode toolkit in `common/`, drawn through `Gfx` (fill, text, item, sprite, scissor, push/pop) which `GfxImpl` in each `version-<mc>` maps to `GuiGraphics` / `GuiGraphicsExtractor`.
`QuestScreen` (per version) is a thin `Screen` that forwards render/mouse/key/char/scroll to `UiRoot`.

- Widgets: `Widget` tree with layout by explicit rects, `Panel`, `Label`, `Button`, `TextField` (single and multi line), `ScrollPane`, `ItemSlot`, `Tabs`, `Checkbox`.
- Views: `ListView` (chapter list, quests with checkboxes, progress) and `GraphView` (pan by drag, zoom by wheel, dependency lines, node sprites by state), same data, toggled by a button.
- `QuestDetail` panel: description (rich text), tasks with progress bars, rewards with claim buttons, item clicks (left: recipes, right: usage via `ViewerBridge`).
- Rich text: small markup (`**bold**`, `*italic*`, `&c` colors, `[text](url)` links, `{item:id}`), rendered by `RichText`.

## Themes (`theme`)

A theme is JSON plus textures in a resource pack: `assets/<ns>/barelyquesting/themes/index.json` lists theme ids, `assets/<ns>/barelyquesting/themes/<id>.json` defines colors,
fonts, sounds and sprites (`panel`, `node.locked|available|progress|complete|claimed`, `line`, `button`, ...), each sprite a texture path plus nine-slice borders.
`ThemeManager` is a client reload listener (F3+T hot reload); the player picks a theme in the book's settings. Ship `default` (plain vanilla-style fills) and `parchment` (warm paper, sprites).
Sprites are drawn with `Gfx.sprite`; a missing sprite falls back to a themed fill, so a partial theme still works.

## In-game editor (`editor`)

An edit mode toggle in the book (only if the server says you may edit). It adds: drag nodes, a palette to add quests, link/unlink dependencies (click the node then another),
a field editor for the selected quest (title, description, icon, position, tasks, rewards, locks) with an item picker (searchable list of registry items, and a BEI/JEI-backed pick when available),
and undo/redo. Every change is an `EditOp`; the editor keeps an inverse op for each so undo/redo are ops too, and live save is automatic because the server saves on every op.
Importers (FTB Quests SNBT, Better Questing JSON) run on the server via `/barelyquesting import <path>` and from the editor.

## Scripting (`script`)

`QuestApi` (MC-free) is the one surface: create chapters/quests, register custom task/reward/lock types (with Java callbacks), listen to events
(`questStarted`, `questCompleted`, `questClaimed`, `taskProgressed`), complete quests, read progress. Two thin bridges expose it:

- **ModJS** (`compat/modjs`): registers a `BindingProvider` through `ExtraBindings.register` so scripts see a `BarelyQuesting` global, and registers event groups.
- **KubeJS** (`compat/kubejs`): a plugin listed in `kubejs.plugins.txt` that adds the `BarelyQuesting` binding and event group.

Both are soft dependencies, loaded only when the mod is present; the mod runs without either.

## Recipe viewers (`compat/viewer`)

`ViewerBridge` in common with `showRecipes(ItemSpec)` / `showUses(ItemSpec)`. Implementations for BEI, JEI, REI and EMI live in their own classes and load only if the viewer is present (reflection or compile-only API), chosen per build by `Compat`.
Optional: a viewer button to open the quest for an item.

## Commands, HUD, toasts

`/barelyquesting open|reset|complete|edit|import|reload|team ...`. Keybind J opens the book (`KeyMapping`), optional craftable Quest Book item. HUD tracker draws pinned quests via `Gfx` from a loader overlay hook; toasts use the vanilla toast manager with themed text and sound.

## Tests

- Unit tests (`common/src/test`): model/JSON roundtrip, store, engine rules (dependencies, locks, claims, teams), each task type, wire format, edit ops with undo/redo, importers, theme parsing, rich text, graph layout, UI hit testing with a recording `Gfx`.
- Headless self test (`version-<mc>/src/dev`, `-PdevTools`, env `BARELYQUESTING_SELFTEST=1`): boots a dedicated server, runs the engine against the real registries (item ids resolve, `McPlayer` matching, command execution, saved files), prints `SELFTEST PASS/FAIL ...` and `SELFTEST DONE failures=0`.
- Not testable headless: rendering, input and real networking. These are listed as unseen in the docs unless a client was run.

## Compatibility standard

- Viewers (BEI, JEI, REI, EMI): the mod adds one item (Quest Book) and a viewer-click feature; availability per build is in `docs/COMPATIBILITY.md`.
- SMWTI/Jade: the mod adds no blocks or entities, so nothing to show.
- ModJS: scripting bridge on every build where ModJS exists. KubeJS: where KubeJS has a build.

## Open questions

- KubeJS and viewer builds for 26.x may not exist yet; those cells are recorded as known gaps, not guessed.
