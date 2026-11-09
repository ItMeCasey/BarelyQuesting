# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-09
Summary: Barely Questing 2026.11.1 builds for all ten Forge and NeoForge targets (1.20.1, 1.21.1, 26.1.2, 26.2, 26.3) and every build passes the headless dedicated-server self test; the screen, editor, themes, HUD, viewer clicks and ModJS/KubeJS bridges were checked on a real client on NeoForge 26.1.2 only; nothing is pushed or published yet.
Spend: none

## Foundation
- [x] DESIGN_BRIEF.md and PLAN.md written (w:2)
- [x] Repository, ten Gradle modules, shared common/ and version-<mc> layout (w:3)
- [~] NeoForge 26.1.2 reference build: compiles, 33 headless checks pass, book opens in a real client through the dev hook; the J keybind itself was not pressed in a real client (w:4)

## Data and server
- [x] Data model and JSON storage under config/barelyquesting (unit-tested, saved and synced in a real client) (w:4)
- [~] Server-authoritative engine, progress and sync (unit tests, headless self test on all ten builds, real client on the reference build; two-client multiplayer not run) (w:5)
- [~] Teams (solo auto team, create/invite/join/leave; unit-tested, not checked in a game) (w:3)
- [x] Task types: item, craft, obtain, kill, advancement, statistic (unit tests plus real registries in the headless test) (w:4)
- [~] Task types: location, dimension, biome, structure, checkmark, scripted (unit-tested; structure and location not exercised in a live world) (w:3)
- [x] Rewards: items, XP, commands, claim flow, team vs player split (unit tests and headless test on real players) (w:3)
- [x] Locks: quest, time, script (unit-tested) (w:2)

## Client UI
- [~] UI toolkit and book screen (unit-tested layout and input; real client on NeoForge 26.1.2 only) (w:5)
- [~] List view and node-graph view (frames read on NeoForge 26.1.2 only) (w:5)
- [~] Quest detail, rich text, claim UI (NeoForge 26.1.2 frames; claim button action failed once in an earlier tour run and was not re-run) (w:3)
- [~] Theme system with default and parchment themes, F3+T reload (both themes seen on NeoForge 26.1.2; the reload key not pressed) (w:4)
- [~] In-game editor with undo and redo, item picker (unit-tested; edit mode, add and undo seen on NeoForge 26.1.2) (w:5)
- [~] Commands, HUD tracker, toasts (HUD and toasts seen on NeoForge 26.1.2; command tree checked headless on all ten) (w:3)
- [ ] Craftable Quest Book item and a viewer button that opens a quest (not built; listed as known gaps) (w:2)

## Integrations
- [~] ModJS bridge (real JavaScript test against a stand-in; real client on NeoForge 26.1.2 with a ModJS development snapshot) (w:3)
- [~] KubeJS bridge (helper-script bridge checked with KubeJS 26.1.2-8.0.8 on a real client; no native plugin, no reload cleanup) (w:3)
- [~] Recipe viewer item clicks (JEI, REI, BEI checked on NeoForge 26.1.2; EMI has no target build; BEI wired on 26.x only) (w:3)
- [~] FTB Quests and Better Questing importer (unit-tested on sample data; not tried against a live book) (w:3)

## Ports
- [~] Forge 26.1.2, NeoForge and Forge 26.2 and 26.3 (compile, unit tests and 33 headless checks pass; screens not seen on a real client) (w:4)
- [~] NeoForge and Forge 1.21.1 (compile and 33 headless checks pass; GuiGraphics layering and tooltip queue not seen on a real client) (w:3)
- [~] NeoForge and Forge 1.20.1 (compile and 33 headless checks pass; screens not seen on a real client) (w:3)

## Testing
- [x] Unit tests for common/ in all ten modules (w:3)
- [x] Headless self test on all ten builds (33 checks each, scripts/server_check.py) (w:4)
- [~] Client check with frames (NeoForge 26.1.2 only; the other nine builds not run) (w:3)

## CI, release and docs
- [x] Scripting API guide, integration matrix, reproduction fixtures and reviewed viewer frames (docs/scripting.md, docs/integrations.md, docs/frames/) (w:2)
- [~] build.yml, release.yml, CurseForge script, issue templates (written, not pushed; CurseForge PROJECT_ID 1794334 set) (w:3)
- [x] Branding, listing text, CurseForge description (branding/, curseforge/DESCRIPTION.md) (w:2)
- [x] Wiki pages written (wiki/barelyquesting; not deployed) (w:3)
- [x] README, CHANGELOG, compatibility and known-gaps docs (w:2)
- [ ] Pushed to GitHub, CI green, released (w:3)
- [ ] CurseForge project created and first release uploaded (w:2)
- [ ] Wiki deployed to itmecasey.com (w:1)
