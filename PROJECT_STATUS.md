# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-06
Summary: Barely Questing (in-game quest book with graph and list views, teams, themes, editor and ModJS/KubeJS scripting) is scaffolded and planned; no feature is built yet.
Spend: none

## Foundation
- [x] DESIGN_BRIEF.md and PLAN.md written (w:2)
- [~] Repository, ten Gradle modules, shared common/ and version-<mc> layout (w:3)
- [ ] NeoForge 26.1.2 reference build compiles, keybind opens an empty book, headless self test passes (w:4)

## Data and server
- [ ] Data model and JSON storage under config/barelyquesting (w:4)
- [ ] Server-authoritative engine, progress and sync (w:5)
- [ ] Teams (solo auto team, create/invite/join/leave) (w:3)
- [ ] Task types: item, craft, obtain, kill, advancement, statistic (w:4)
- [ ] Task types: location, dimension, biome, structure, checkmark, scripted (w:3)
- [ ] Rewards: items, XP, commands, claim flow, team vs player split (w:3)
- [ ] Locks: quest, time, script (w:2)

## Client UI
- [ ] UI toolkit and book screen (w:5)
- [ ] List view and node-graph view (w:5)
- [ ] Quest detail, rich text, claim UI (w:3)
- [ ] Theme system with default and parchment themes, F3+T reload (w:4)
- [ ] In-game editor with undo and redo, item picker (w:5)
- [ ] Commands, HUD tracker, toasts, quest book item (w:3)

## Integrations
- [ ] ModJS bridge (w:3)
- [ ] KubeJS bridge (w:3)
- [ ] Recipe viewer item clicks (BEI, JEI, REI, EMI) (w:3)
- [ ] FTB Quests and Better Questing importer (w:3)

## Ports
- [ ] Forge 26.1.2, NeoForge and Forge 26.2 and 26.3 (w:4)
- [ ] NeoForge and Forge 1.21.1 (w:3)
- [ ] NeoForge and Forge 1.20.1 (w:3)

## Testing
- [ ] Unit tests for common/ in all ten modules (w:3)
- [ ] Headless self test on all ten builds (w:4)
- [ ] Client check with frames (w:3)

## CI, release and docs
- [ ] build.yml, release.yml, CurseForge script, issue templates (built, not pushed) (w:3)
- [ ] Branding, listing text, CurseForge description (w:2)
- [ ] Wiki pages written (not deployed) (w:3)
- [ ] README, CHANGELOG, compatibility and known-gaps docs (w:2)
- [ ] Pushed to GitHub, CI green, released (owned by the project manager session) (w:3)
- [ ] CurseForge project created and first release uploaded (w:2)
