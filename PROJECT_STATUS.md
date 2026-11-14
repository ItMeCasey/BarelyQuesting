# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-14
Summary: Barely Questing 2026.11.9 (committed locally, not pushed; 2026.11.8 is released) builds for all ten Forge and NeoForge targets; every build passed unit tests, the headless server self test and a real-client tour, with deeper checks (editor, teams, tasks in a live world, viewers, ModJS 2026.11.31, KubeJS reload, real FTB Quests and Better Questing books) listed per step; open: CurseForge approval.
Spend: none

## Foundation
- [x] DESIGN_BRIEF.md and PLAN.md written (w:2)
- [x] Repository, ten Gradle modules, shared common/ and version-<mc> layout (w:3)
- [x] NeoForge 26.1.2 reference build: compiles, 33 headless checks pass, book opens in a real client, and the J keybind path (KeyMapping.click, the keyboard handler's own call) opens the book on all ten builds (w:4)

## Data and server
- [x] Data model and JSON storage under config/barelyquesting (unit-tested, saved and synced in a real client) (w:4)
- [x] Server-authoritative engine, progress and sync (unit tests, headless self test on all ten, real client on all ten; a second player is simulated through the server API because two real clients were not run) (w:5)
- [x] Teams (unit-tested; in a real client on all ten builds: create through the UI, a simulated second member invited and joined through the server API, shared completion seen by both, disband through the UI) (w:3)
- [x] Task types: item, craft, obtain, kill, advancement, statistic (unit tests plus real registries in the headless test) (w:4)
- [x] Task types: location, dimension, biome, structure, checkmark, scripted (location, dimension, biome, structure via a naturally generated ruined portal, and checkmark through the Check button, in a live world on NeoForge 26.1.2, 26.2, 26.3, 1.21.1 and 1.20.1; scripted through ModJS and KubeJS callbacks) (w:3)
- [x] Rewards: items, XP, commands, claim flow, team vs player split (unit tests and headless test on real players) (w:3)
- [x] Locks: quest, time, script (unit-tested) (w:2)

## Client UI
- [x] UI toolkit and book screen (unit-tested layout and input; real client on all ten builds) (w:5)
- [x] List view and node-graph view (List and Graph pressed through the screen's handlers on all ten builds; frames read on NeoForge 26.1.2, 26.2 and 1.21.1) (w:5)
- [x] Quest detail, rich text, claim UI (Claim all pressed and the reward claimed on all ten builds; the earlier failure was the tour leaving edit mode on) (w:3)
- [x] Theme system with default and parchment themes, F3+T reload (theme switch plus the resource reload F3+T runs, twice, on all ten builds; the key combination itself was not pressed) (w:4)
- [x] In-game editor with undo and redo, item picker (add, undo, redo, undo; the picker driven by typing and Enter through the screen's key and char handlers; all ten builds) (w:5)
- [x] Commands, HUD tracker, toasts (reload, reset, complete, open, bq edit and the HUD with a pinned quest and a toast, on all ten builds) (w:3)
- [x] Craftable Quest Book item (held in the main hand and used through the game mode on all ten builds; the book opened) (w:2)
- [x] Ctrl+J over an item opens its quest (key state set through the game over an inventory slot on all ten builds; the hovered item read out of live JEI, REI and BEI overlays on NeoForge 26.1.2) (w:1)
- [x] EMI, JEI, REI and BEI on 1.20.1 and 1.21.1: recipe and usage clicks plus the Ctrl+J hover read, real clients with CurseForge jars (NeoForge 1.21.1: JEI, REI, EMI, BEI; Forge 1.21.1: JEI, BEI; Forge 1.20.1: JEI, REI, EMI, BEI; NeoForge 1.20.1: BEI); n/a: no EMI build for NeoForge 26.1.2, no JEI, REI or EMI build for NeoForge 1.20.1, no REI or EMI build for Forge 1.21.1 (w:2)

## Integrations
- [x] ModJS bridge (real JavaScript test, and a real client on NeoForge 26.1.2 with ModJS 2026.11.31, sha256 c9944fde...; same reflective code on every build) (w:3)
- [x] KubeJS bridge (helper script checked on a real client with KubeJS 26.1.2-8.0.8; reload cleanup through clearListeners(): two real /reloads left one listener; a native plugin is not built by design) (w:3)
- [x] Recipe viewer item clicks (JEI, REI and BEI recipes and uses on NeoForge 26.1.2) (w:3)
- [x] FTB Quests and Better Questing importer (a real FTB Quests chapter and a real Better Questing split export imported in unit tests; they exposed two importer gaps, now fixed; the Better Questing slice had no tasks or rewards) (w:3)

## Ports
- [x] Forge 26.1.2, NeoForge and Forge 26.2 and 26.3 (compile, unit tests, 33 headless checks and a real-client tour pass) (w:4)
- [x] NeoForge and Forge 1.21.1 (compile, 33 headless checks and a real-client tour pass; frame checked, layering fine; Forge 1.21.1 tour failed twice on a race before the command step, then passed) (w:3)
- [x] NeoForge and Forge 1.20.1 (compile, 33 headless checks and a real-client tour pass; Forge jar reobfuscated, 241 SRG names; both jars load in real Forge 47.4.28 and NeoForge 47.1.106 installs) (w:3)

## Testing
- [x] Unit tests for common/ in all ten modules (w:3)
- [x] Headless self test on all ten builds (33 checks each, scripts/server_check.py) (w:4)
- [x] Client check with frames (all ten builds: NeoForge 26.1.2 fullest; the other nine ran the tour once) (w:3)

## CI, release and docs
- [x] Scripting API guide, integration matrix, reproduction fixtures and reviewed viewer frames (docs/scripting.md, docs/integrations.md, docs/frames/) (w:2)
- [x] build.yml, release.yml, CurseForge script, issue templates (pushed; CI green on build and release runs; CurseForge PROJECT_ID 1794334 and token set) (w:3)
- [~] CI builds and releases only the versions that changed (plan job and tested path rules written and committed; not yet seen running on GitHub) (w:1)
- [x] Branding, listing text, CurseForge description (branding/, curseforge/DESCRIPTION.md) (w:2)
- [x] Wiki pages written (wiki/barelyquesting; deployed, matches the site copy) (w:3)
- [x] README, CHANGELOG, compatibility and known-gaps docs (w:2)
- [x] Pushed to GitHub (public repo ItMeCasey/BarelyQuesting, CI green, tag v2026.11.1, prerelease with ten jars) (w:3)
- [x] CurseForge project 1794334 created; 2026.11.5 uploaded (ten files) and approved (2026-11-11) (w:2)
- [x] Wiki deployed to itmecasey.com (live, pages return 200) (w:1)
- [x] 2026.11.6 (Forge and NeoForge 26.3 only): mouse button fix for the 26.3 renumbering; tried in a real NeoForge 26.3 client with 2026.11.7 (clicks and Esc work) (w:1)
- [x] Issue #1: the x in every dialog closes it (click catcher no longer covers it; client tour presses the x on Themes and Team on NeoForge 1.21.1 and 26.3) and, on 26.3, Esc and the book's other keys work (SDL scancodes translated; Esc sent through the screen's real key handler on NeoForge 26.3) (w:2)
- [x] Issue #4: long lines in the Themes and Team dialogs wrap inside the box (Ui.wrap, Ui.paragraph; unit test on text bounds; frames on NeoForge 1.21.1 and 26.3) (w:1)
- [x] Issue #2: `"editing": "off"` in book.json hides Edit and refuses edits for everyone, operators included; default on (unit tests; real client: operator has no Edit button and canEdit is false on NeoForge 1.21.1) (w:2)
- [x] Issue #3: `give_book_on_join` and `open_book_on_join` in book.json, per-world first-join list, defaults off (unit tests; real client: first join got one Quest Book and the book opened on NeoForge 1.21.1) (w:2)
- [x] No hints in the book (Casey, 2026-11-13): empty-state captions, Themes and Team helper lines, editor prompts and extra tooltip lines, the Import button, the starter book's "Press J" line removed on all ten builds; frames on NeoForge 1.21.1 and Forge 1.21.1 (w:2)
- [x] ModJS bridge reviewed against ModJS 2026.11.38 (ExtraBindings.register / BindingProvider unchanged); client scripts no longer get the BarelyQuesting global, unit test added (w:1)
- [x] Settings in the loader's standard config files: barelyquesting-common.toml (barelyquesting-startup.toml on NeoForge 26.3) and barelyquesting-client.toml, built from one key list (ConfigKeys), NeoForge in-game config screen; old book.json and client.json settings migrated to the toml with a .back copy of the old file; 17 unit tests with real files from 2026.11.1 and 2026.11.7; real clients on NeoForge 1.21.1 and Forge 1.21.1 migrated the dev config and loaded the toml (w:4)
- [ ] 2026.11.9 on the other eight builds: shared code and all nine loader config classes compile; real-client checks ran on NeoForge 1.21.1 and Forge 1.21.1 only; the NeoForge config screen was compiled but not opened; CI builds the rest (w:1)
