# Changelog

## 2026.11.4

- Importers: Better Questing exports that keep one JSON file per quest and quest line (`DefaultQuests/Quests/...`, `QuestLines/...`) now import; FTB Quests colour codes such as `&a` no longer show in chapter and quest titles. Both found by importing real exported books.
- Scripts: `BarelyQuesting.clearListeners()` so a KubeJS or ModJS script reload replaces listeners instead of adding copies.
- Checked on real clients on all ten builds: theme switch with a resource reload, editor undo/redo and item picker, commands, HUD and toast, the Quest Book item held in hand, Ctrl+J over an inventory slot, and team create, invite, join, shared progress and disband. Location, dimension, biome, structure and checkmark tasks were exercised in a live world on one build per Minecraft version.
- Recipe viewers: BEI now opens from the quest panel on the 1.20.1 and 1.21.1 builds too (it was only wired on 26.x). Recipe and usage clicks and the Ctrl+J hover read were checked on real clients with JEI, REI, EMI and BEI on every 1.20.1 and 1.21.1 loader where that viewer exists.
- The item picker takes Enter to choose the first match.

## 2026.11.3

- Ctrl+J over an item in a recipe viewer (JEI, REI, EMI, BEI) or an inventory slot opens the quest that asks for, rewards or shows that item.
- Client tours now exist for 1.20.1 and 1.21.1 and every build has been run once on a real client.

## 2026.11.2

- Added the craftable Quest Book item (a book and a gold nugget, shapeless). Right-click opens the book; it has its own creative tab.
- Fixed the Forge 1.20.1 jar: it now ships with the names the real game uses (it was unmapped and would have crashed outside a development run).
- Fixed the development client tour so it leaves edit mode before pressing Claim all (the button is not shown in edit mode, which was the earlier "claim failure").

## 2026.11.1

First release.

- Quest book with a node-graph view and a list view, chapters, quests, dependencies and locks.
- Tasks: item, craft, obtain, kill, advancement, statistic, location, dimension, biome, structure, checkmark, scripted.
- Rewards: items, XP and commands, claimed in the book (player or team scope).
- Per-player progress with shared teams; the server owns the data and syncs it.
- Resource-pack themes (Default and Parchment), F3+T reload.
- In-game editor with undo and redo; live save to JSON files in `config/barelyquesting/`.
- Commands, HUD tracker, toasts.
- ModJS and KubeJS scripting, recipe viewer clicks (JEI, REI, EMI, BEI), FTB Quests and Better Questing importers.
- Ten builds: Forge and NeoForge for Minecraft 1.20.1, 1.21.1, 26.1.2, 26.2 and 26.3.
