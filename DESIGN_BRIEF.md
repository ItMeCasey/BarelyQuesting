# Barely Questing: design brief (from Casey, 2026-11-07)

An in-game questing guide for Minecraft, like Better Questing or FTB Quests, in the Barely family (BarelyPipes, BarelyStorage, BarelyPowered, BarelySorts).

## Targets
Forge and NeoForge on Minecraft 1.20.1, 1.21.1, 26.1.2, 26.2 and 26.3: ten builds, one jar per loader and Minecraft version (ten separate jars, as Barely Pipes ships). Shared code in `common/`, thin per-version glue. Version `YYYY.M.increment`, first release 2026.11.1.

## Quest structure
- Chapters containing quests. Two switchable views of the same data: a pannable node graph with dependency lines, and a list view with checkboxes.
- Quests: title, description (rich text), icon, position, dependencies, tasks, rewards, locks.
- Locks: other quests, time, or a script condition.

## Tasks (first release)
- Item (submit or have, with NBT and tag matching), Craft, Obtain.
- Kill, Advancement, Statistic.
- Location, Dimension, Biome (and structure).
- Checkmark (manual) and Scripted (defined by ModJS/KubeJS).

## Rewards
Items, XP and commands (basics), plus a reward claiming UI (players claim manually; team vs player split).

## Teams and progress
Per-player progress, plus shared teams (players form teams that share progress). Server-authoritative, synced to clients.

## Themes
Resource-pack themes: JSON plus textures define colors, fonts, panel and node sprites, sounds; hot-reload with F3+T. Ship a default theme and one alternative so themes are proven.

## In-game editor
- Graph editing: drag nodes, add and link quests, undo and redo.
- Field editing: tasks, rewards, text, icons, with item pickers backed by BEI/JEI.
- Live save: edits apply instantly for everyone online and save as files in the config folder.
- Import: convert FTB Quests and Better Questing books once (no runtime dependency on them).
- Permission-gated (ops / creative / an editor permission).

## Storage
JSON files per chapter under `config/barelyquesting/` (readable, diff-friendly, hand-editable, git-friendly), plus a small metadata file. Per-world progress data separate from the quest definitions.

## Scripting (ModJS and KubeJS)
Full support, both as optional soft dependencies: scripts can create quests and chapters, register custom task and reward types, listen to events (quest started, completed, claimed, task progressed) and complete quests. ModJS is our own mod (`C:\Users\casey\Desktop\ModJS`, wiki at itmecasey.com/wiki/modjs): follow its extension pattern; KubeJS through its public API. The mod must work without either installed.

## Recipe viewers
Click any quest item to open recipe and usage pages in BEI/JEI/EMI/REI (soft dependencies, per-build availability as in `Mod-Playbook/market-research`). BEI is ours (`C:\Users\casey\Desktop\BEI`, `bei-api` on the private Maven). Optional: a button in the viewer to open the quest for an item.

## Player UI
Keybind (default J) opens the book; an optional craftable quest book item opens it too; HUD tracker for pinned quests with progress; toasts and sounds (themeable); `/barelyquesting` commands (open, reset, complete, edit, import).

## Not in scope for the first release
Loot-table or choose-one rewards, a viewer category listing quests, runtime compatibility with other quest mods.
