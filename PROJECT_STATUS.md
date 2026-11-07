# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-07
Summary: Barely Questing has a NeoForge 26.1.2 reference build with a 33-check headless self-test, shared quest engine, UI toolkit, importers and optional scripting/viewer bridges; ports and real-client checks remain open.
Spend: none

## Foundation
- [x] DESIGN_BRIEF.md and PLAN.md written (w:2)
- [x] Repository, ten Gradle modules, shared common/ and version-<mc> layout (w:3)
- [~] NeoForge 26.1.2 reference build compiles and passes 33 headless self-test checks; the keybind and book screen are not yet checked in a real client (w:4)

## Data and server
- [~] Data model and JSON storage under config/barelyquesting (implemented and unit-tested; not inspected in a running game) (w:4)
- [~] Server-authoritative engine, progress and sync (implemented and covered by unit and headless self-tests; no real client networking check) (w:5)
- [~] Teams (solo auto team, create/invite/join/leave; unit-tested, not checked in a game) (w:3)
- [~] Task types: item, craft, obtain, kill, advancement, statistic (implemented and unit-tested) (w:4)
- [~] Task types: location, dimension, biome, structure, checkmark, scripted (implemented and unit-tested) (w:3)
- [~] Rewards: items, XP, commands, claim flow, team vs player split (implemented and unit-tested) (w:3)
- [~] Locks: quest, time, script (implemented and unit-tested) (w:2)

## Client UI
- [~] UI toolkit and book screen (implemented with unit-tested layout and input; not checked in a real client) (w:5)
- [~] List view and node-graph view (implemented and unit-tested; not checked in a real client) (w:5)
- [~] Quest detail, rich text, claim UI (implemented and unit-tested; not checked in a real client) (w:3)
- [~] Theme system with default and parchment themes, F3+T reload (implemented; not checked in a real client) (w:4)
- [~] In-game editor with undo and redo, item picker (implemented and unit-tested; not checked in a real client) (w:5)
- [~] Commands, HUD tracker, toasts, quest book item (implemented and headless-tested where possible; not checked in a real client) (w:3)

## Integrations
- [~] ModJS bridge (optional reflective binding implemented and unit-tested on the reference build; not checked with ModJS installed) (w:3)
- [~] KubeJS bridge (optional helper-script bridge implemented; not checked with KubeJS installed) (w:3)
- [~] Recipe viewer item clicks (optional EMI, REI and JEI reflection bridge implemented; BEI has no public lookup API; no viewer client check) (w:3)
- [~] FTB Quests and Better Questing importer (implemented and unit-tested; not tried against a live book) (w:3)

## Ports
- [ ] Forge 26.1.2, NeoForge and Forge 26.2 and 26.3 (w:4)
- [ ] NeoForge and Forge 1.21.1 (w:3)
- [ ] NeoForge and Forge 1.20.1 (w:3)

## Testing
- [~] Unit tests for common/ in all ten modules (pass on the NeoForge 26.1.2 reference build; ports unverified) (w:3)
- [~] Headless self test on all ten builds (NeoForge 26.1.2 passes 33 checks; ports unverified) (w:4)
- [ ] Client check with frames (w:3)

## CI, release and docs
- [~] build.yml, release.yml, CurseForge script, issue templates (built locally, not pushed) (w:3)
- [ ] Branding, listing text, CurseForge description (w:2)
- [ ] Wiki pages written (not deployed) (w:3)
- [ ] README, CHANGELOG, compatibility and known-gaps docs (w:2)
- [ ] Pushed to GitHub, CI green, released (owned by the project manager session) (w:3)
- [ ] CurseForge project created and first release uploaded (w:2)
