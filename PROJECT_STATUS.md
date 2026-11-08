# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-08
Summary: NeoForge 26.1.2 builds and passes shared tests plus 33 standalone headless checks (35 with ModJS). KubeJS startup creation/callback/client sync and BEI/JEI/REI lookups have real-client evidence; ModJS 2026.11.30 development snapshot also passed fresh callback/client-sync checks; all other ports remain open.
Spend: none

## Foundation
- [x] DESIGN_BRIEF.md and PLAN.md written (w:2)
- [x] Repository, ten Gradle modules, shared common/ and version-<mc> layout (w:3)
- [~] NeoForge 26.1.2 reference build compiles and passes 33 headless self-test checks; the book opens in a real client through the dev hook; the keybind is not checked (w:4)

## Data and server
- [~] Data model and JSON storage under config/barelyquesting (implemented and unit-tested; script-created content was saved and synced in an integrated client) (w:4)
- [~] Server-authoritative engine, progress and sync (implemented and covered by unit/headless tests; script-created quest sync checked in an integrated client, multiplayer unverified) (w:5)
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
- [~] ModJS bridge (optional binding unit-tested; ModJS 2026.11.30 development snapshot passes 35 dedicated-server checks plus real-client startup creation, reset-and-poll callback and progress sync; older .23 client payload blocker resolved in this snapshot) (w:3)
- [~] KubeJS bridge (helper-script bridge checked with KubeJS 26.1.2-8.0.8 in a real client: startup creation, callback completion and sync; native plugin/reload cleanup not implemented) (w:3)
- [~] Recipe viewer item clicks (BEI, REI and JEI recipes/usages checked in real clients with inspected frames; isolated fallback tested; EMI retained but no compatible NeoForge 26.1.2 build listed by Modrinth; BEI uses an internal popup adapter) (w:3)
- [~] FTB Quests and Better Questing importer (implemented and unit-tested; not tried against a live book) (w:3)

## Ports
- [ ] Forge 26.1.2, NeoForge and Forge 26.2 and 26.3 (w:4)
- [ ] NeoForge and Forge 1.21.1 (no loader entrypoint or version adapter exists yet; a trial copy of the 26.1.2 adapter failed to compile against 1.21.1 APIs and was removed) (w:3)
- [ ] NeoForge and Forge 1.20.1 (w:3)

## Testing
- [~] Unit tests for common/ in all ten modules (58 tests pass on the NeoForge 26.1.2 reference build; ports unverified) (w:3)
- [~] Headless self test on all ten builds (NeoForge 26.1.2 passes 33 checks without optional mods and 35 with ModJS; ports unverified) (w:4)
- [~] Client check with frames (KubeJS creation/callback/sync and BEI/JEI/REI recipes/usages plus no-optional-mod fallback checked; ModJS 2026.11.30 fresh callback/client-sync check also passed; general keybind/editor/reward acceptance remains open) (w:3)

## CI, release and docs
- [x] Scripting API guide, integration matrix, reproduction fixtures and reviewed viewer frames (docs/scripting.md, docs/integrations.md, docs/frames/) (w:2)
- [~] build.yml, release.yml, CurseForge script, issue templates (built locally, not pushed) (w:3)
- [ ] Branding, listing text, CurseForge description (w:2)
- [ ] Wiki pages written (not deployed) (w:3)
- [ ] README, CHANGELOG, compatibility and known-gaps docs (w:2)
- [ ] Pushed to GitHub, CI green, released (owned by the project manager session) (w:3)
- [ ] CurseForge project created and first release uploaded (w:2)
