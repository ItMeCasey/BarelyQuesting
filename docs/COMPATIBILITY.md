# Compatibility and known gaps

## What was checked, per build

| Build | Compiles | Unit tests (common) | Headless server self test (33 checks) | Real client |
|---|---|---|---|---|
| NeoForge 26.1.2 | yes | yes | pass | yes (reference build: book, themes, editor, toasts, HUD, viewers, ModJS, KubeJS; see docs/frames) |
| Forge 26.1.2 | yes | yes | pass | not run |
| NeoForge 26.2 | yes | yes | pass | not run |
| Forge 26.2 | yes | yes | pass | not run |
| NeoForge 26.3 | yes | yes | pass | not run |
| Forge 26.3 | yes | yes | pass | not run |
| NeoForge 1.21.1 | yes | yes | pass | not run |
| Forge 1.21.1 | yes | yes | pass | not run |
| NeoForge 1.20.1 | yes | yes | pass | not run |
| Forge 1.20.1 | yes | yes | pass | not run |

The self test starts a dedicated server with the mod, creates a real `ServerPlayer`, and checks item and tag matching, item data, XP, commands run as the player, the engine on real registries (obtain, craft, claim, submit, kill), saving, and the command tree. Rendering, input, the keybind and real multiplayer networking are only checked on the reference build; every other build's screen is "not seen on a real client".

## Optional mods

| Mod | Role | Status |
|---|---|---|
| ModJS | scripting global `BarelyQuesting` | checked with real JavaScript against a stand-in of its binding API, and on a real client on NeoForge 26.1.2 with a ModJS 2026.11.30 development snapshot. Same reflective code on every build. |
| KubeJS | scripting through a generated helper script | checked on NeoForge 26.1.2 with KubeJS 26.1.2-8.0.8. KubeJS has builds for 1.20.1 (Forge, NeoForge), 1.21.1 (NeoForge) and 26.1.2 (NeoForge); none for 26.2, 26.3 or Forge 1.21.1/26.x. No native plugin: callback registrations are not cleaned up on a script reload, restart after changing them. |
| JEI, REI, EMI, BEI | item clicks open recipes and uses | JEI, REI and BEI checked on NeoForge 26.1.2. EMI has no build for that target, so it is untested. BEI is only wired on the 26.x builds (its lookup is an internal popup); on 1.20.1 and 1.21.1 BEI is not driven. JEI reaches its runtime through an internal class, so a JEI change can break it. |

The mod adds no blocks or entities, so SMWTI and Jade have nothing to show.

## Known gaps

- The craftable Quest Book item from the design brief is not built; the book opens with the J key and `/barelyquesting open`.
- A viewer button that opens the quest for an item is not built.
- The editor's item picker lists registry items; it is not backed by JEI or BEI's own search.
- The 1.20.1 and 1.21.1 screens draw by depth instead of by layer; their frames have not been looked at.
- Multiplayer (two clients on one server) is covered by unit tests of the wire format and server, not by a real session.
