# Compatibility and known gaps

## What was checked, per build

| Build | Compiles | Unit tests (common) | Headless server self test (33 checks) | Real client |
|---|---|---|---|---|
| NeoForge 26.1.2 | yes | yes | pass | yes (reference build: book, themes, editor, toasts, HUD, viewers, ModJS, KubeJS; see docs/frames) |
| Forge 26.1.2 | yes | yes | pass | yes (tour: book, graph and list, themes, editor, team, claim, HUD, toast, item-to-quest lookup) |
| NeoForge 26.2 | yes | yes | pass | yes (tour: book, graph and list, themes, editor, team, claim, HUD, toast, item-to-quest lookup) |
| Forge 26.2 | yes | yes | pass | yes (tour: book, graph and list, themes, editor, team, claim, HUD, toast, item-to-quest lookup) |
| NeoForge 26.3 | yes | yes | pass | yes (tour: book, graph and list, themes, editor, team, claim, HUD, toast, item-to-quest lookup) |
| Forge 26.3 | yes | yes | pass | yes (tour: book, graph and list, themes, editor, team, claim, HUD, toast, item-to-quest lookup) |
| NeoForge 1.21.1 | yes | yes | pass | yes (tour: book, graph and list, themes, editor, team, claim, HUD, toast, item-to-quest lookup) |
| Forge 1.21.1 | yes | yes | pass | yes (tour: book, graph and list, themes, editor, team, claim, HUD, toast, item-to-quest lookup) |
| NeoForge 1.20.1 | yes | yes | pass | yes (tour: book, graph and list, themes, editor, team, claim, HUD, toast, item-to-quest lookup) |
| Forge 1.20.1 | yes | yes | pass | yes (tour: book, graph and list, themes, editor, team, claim, HUD, toast, item-to-quest lookup) |

The self test starts a dedicated server with the mod, creates a real `ServerPlayer`, and checks item and tag matching, item data, XP, commands run as the player, the engine on real registries (obtain, craft, claim, submit, kill), saving, and the command tree. Every build then ran the development client tour once on a real client (isolated desktop, input driven through the game's own handlers): it opens the book, presses the buttons, claims a reward and checks the item-to-quest lookup. NeoForge 26.1.2 has the fullest check (viewers and scripting bridges). Not run on any build: a real keypress of J or Ctrl+J, and two clients on one server. Forge 1.20.1 and NeoForge 1.20.1 jars were also loaded in real Forge 47.4.28 and NeoForge 47.1.106 installs (mod found, entry class created, client reached the title screen, no mapping errors); the Forge 1.20.1 jar is reobfuscated (241 SRG names).

## Optional mods

| Mod | Role | Status |
|---|---|---|
| ModJS | scripting global `BarelyQuesting` | checked with real JavaScript against a stand-in of its binding API, and on a real client on NeoForge 26.1.2 with ModJS 2026.11.31 (jar sha256 c9944fde5c545cca323ee5edcce064a856b242b3ccc13c09052404cfe2818af0). Same reflective code on every build. |
| KubeJS | scripting through a generated helper script | checked on NeoForge 26.1.2 with KubeJS 26.1.2-8.0.8. KubeJS has builds for 1.20.1 (Forge, NeoForge), 1.21.1 (NeoForge) and 26.1.2 (NeoForge); none for 26.2, 26.3 or Forge 1.21.1/26.x. There is no native KubeJS plugin by design (the helper script needs no KubeJS version). Script reloads are handled by `BarelyQuesting.clearListeners()` at the top of a server script: two real `/reload`s on KubeJS 8.0.8 left exactly one listener firing. |
| JEI, REI, EMI, BEI | item clicks open recipes and uses | Checked on real clients, both the click-through to recipes and uses and Ctrl+J reading the hovered item out of each overlay with an empty inventory: NeoForge 26.1.2 (JEI, REI, BEI), NeoForge 1.21.1 (JEI, REI, EMI, BEI), Forge 1.21.1 (JEI, BEI), Forge 1.20.1 (JEI, REI, EMI, BEI), NeoForge 1.20.1 (BEI). n/a: no EMI build for NeoForge 26.1.2, no JEI, REI or EMI build for NeoForge 1.20.1, no REI or EMI build for Forge 1.21.1. BEI is driven through its popup class by reflection. JEI reaches its runtime through an internal class, so a JEI change can break it. |

The mod adds no blocks or entities, so SMWTI and Jade have nothing to show.

## Known gaps

- Ctrl+J over an item opens its quest. Checked on every build with the key state set through the game (not an OS keypress) over an inventory slot, and over the JEI, REI, EMI and BEI overlays on every build where that viewer exists (see the viewer row above).
- The editor's item picker lists registry items; it is not backed by JEI or BEI's own search.
- The 1.20.1 and 1.21.1 screens draw by depth instead of by layer; their frames have not been looked at.
- Multiplayer (two real clients on one server) was not run. A second player is simulated through the server API in unit tests and in a real client (team create, invite, join, shared completion, disband).
- The importers were run on one real FTB Quests chapter (Forge 1.20.1 format) and one real Better Questing split export (format 3.1.0); both exposed gaps that are fixed in 2026.11.4. The Better Questing sample was a small slice, so tasks and rewards from real Better Questing books are covered only by the hand-written sample.
