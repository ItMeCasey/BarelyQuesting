# Changelog

## 2026.11.8

- Removed the hint text from the book: the empty-state captions, the Themes and Team dialog helper lines (the Team header now just says Solo), the editor's prompts and extra tooltip lines, the Import button that only printed a command, and the first-quest "Press J" line in the starter book. Names, values and short factual messages stay.
- ModJS: client scripts no longer get the `BarelyQuesting` global (the quest engine runs on the server); startup and server scripts are unchanged.

## 2026.11.7

- Fixed: the **x** in the corner of every dialog (Themes, Team, item picker and the rest) did nothing, because the dialog's click catcher sat on top of it. It now closes the dialog on every build. ([#1](https://github.com/ItMeCasey/BarelyQuesting/issues/1))
- Fixed (Minecraft 26.3): **Esc** did not close a dialog and Enter, Backspace, the arrow keys and Ctrl+A, C, V, X, Y, Z did nothing in the book, because 26.3 reports keys as SDL scancodes instead of GLFW codes. The 26.3 builds now translate them. ([#1](https://github.com/ItMeCasey/BarelyQuesting/issues/1))
- Fixed: text ran past the edge of the Themes and Team dialogs. Long lines now wrap inside the box (new `Ui.wrap` and `Ui.paragraph`). ([#4](https://github.com/ItMeCasey/BarelyQuesting/issues/4))
- New: `"editing": "off"` in `book.json` hides the Edit button and refuses every edit for everyone, operators included. Default `"on"`. Scripts, import and `/barelyquesting reload` still work. See the Book settings wiki page. ([#2](https://github.com/ItMeCasey/BarelyQuesting/issues/2))
- New: `give_book_on_join` gives each player a Quest Book the first time they join a world, and `open_book_on_join` opens the book then. Both default to `false`, so nothing changes unless a pack turns them on. ([#3](https://github.com/ItMeCasey/BarelyQuesting/issues/3))

## 2026.11.6 (Forge and NeoForge 26.3 only)

- Fixed: clicks in the quest book on Minecraft 26.3 used the wrong buttons (left acted as right, right did nothing). Minecraft 26.3 renumbered the mouse buttons (left is now 1, middle 2, right 3), and the mod still compared clicks against 0 and 1. The other eight builds are unchanged and stay at 2026.11.5.

## 2026.11.5

- Recipe viewers: BEI now opens from the quest panel on the 1.20.1 and 1.21.1 builds too (it was only wired on 26.x). Recipe and usage clicks and the Ctrl+J hover read were checked on real clients with JEI, REI, EMI and BEI on every 1.20.1 and 1.21.1 loader where that viewer exists.

## 2026.11.4

- Importers: Better Questing exports that keep one JSON file per quest and quest line (`DefaultQuests/Quests/...`, `QuestLines/...`) now import; FTB Quests colour codes such as `&a` no longer show in chapter and quest titles. Both found by importing real exported books.
- Scripts: `BarelyQuesting.clearListeners()` so a KubeJS or ModJS script reload replaces listeners instead of adding copies.
- Checked on real clients on all ten builds: theme switch with a resource reload, editor undo/redo and item picker, commands, HUD and toast, the Quest Book item held in hand, Ctrl+J over an inventory slot, and team create, invite, join, shared progress and disband. Location, dimension, biome, structure and checkmark tasks were exercised in a live world on one build per Minecraft version.
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
