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
| ModJS | scripting global `BarelyQuesting` | checked with real JavaScript against a stand-in of its binding API, and on a real client on NeoForge 26.1.2 with a ModJS 2026.11.30 development snapshot. Same reflective code on every build. |
| KubeJS | scripting through a generated helper script | checked on NeoForge 26.1.2 with KubeJS 26.1.2-8.0.8. KubeJS has builds for 1.20.1 (Forge, NeoForge), 1.21.1 (NeoForge) and 26.1.2 (NeoForge); none for 26.2, 26.3 or Forge 1.21.1/26.x. No native plugin: callback registrations are not cleaned up on a script reload, restart after changing them. |
| JEI, REI, EMI, BEI | item clicks open recipes and uses | JEI, REI and BEI checked on NeoForge 26.1.2. EMI has no build for that target, so it is untested. BEI is only wired on the 26.x builds (its lookup is an internal popup); on 1.20.1 and 1.21.1 BEI is not driven. JEI reaches its runtime through an internal class, so a JEI change can break it. |

The mod adds no blocks or entities, so SMWTI and Jade have nothing to show.

## Known gaps

- The Quest Book item (2026.11.2) is registered on all ten builds and compiles and loads in the headless test, but it has not been held in a real client.
- Ctrl+J over an item opens its quest (2026.11.3). The lookup and the book opening were checked on real clients; reading the hovered item out of JEI, REI, EMI or BEI is reflection against their current APIs and was not tried in a live viewer session. Inventory slots use the game's own hovered slot.
- The editor's item picker lists registry items; it is not backed by JEI or BEI's own search.
- The 1.20.1 and 1.21.1 screens draw by depth instead of by layer; their frames have not been looked at.
- Multiplayer (two clients on one server) is covered by unit tests of the wire format and server, not by a real session.
