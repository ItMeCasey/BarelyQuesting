---
title: Versions and known gaps
summary: What was checked on each build, and what was not.
---
All ten builds compile, run the shared unit tests and pass a headless dedicated-server self test (a real player, item and tag matching, the engine, saving, commands). Every build was also run once on a real client (open the book, graph and list, themes, editor, team, claim a reward, HUD and toasts, find a quest by item). The ModJS and KubeJS bridges were run on **NeoForge 26.1.2** only. Recipe viewers were run on NeoForge 26.1.2, NeoForge 1.21.1, Forge 1.21.1, Forge 1.20.1 and NeoForge 1.20.1, with every viewer that exists for each (JEI, REI, EMI, BEI). The Forge and NeoForge 1.20.1 jars were also loaded in real Forge and NeoForge installs.

| Build | Server self test | Real client |
|---|---|---|
| NeoForge 26.1.2 | pass | yes, plus viewers and scripting |
| Forge 26.1.2, NeoForge and Forge 26.2, 26.3 | pass | yes |
| NeoForge and Forge 1.21.1 | pass | yes |
| NeoForge and Forge 1.20.1 | pass | yes, and a real Forge and NeoForge install |

## Known gaps

- The Quest Book item (a book and a gold nugget) is new in 2026.11.2 and has not been tried in a real client. J and `/barelyquesting open` always work.
- Ctrl+J over an item opens its quest. Checked over an inventory slot on every build and over the JEI, REI, EMI and BEI overlays on every build where that viewer exists.
- The editor's item picker lists registry items; it is not driven by JEI or BEI search.
- KubeJS support is a helper script, not a native plugin, and KubeJS has no build for 26.2, 26.3, or Forge 1.21.1 and 26.x. EMI has no build for NeoForge 26.1.2 or NeoForge 1.20.1, and REI and EMI have none for Forge 1.21.1.
- BEI lookups are wired on the 26.x builds only.
- Multiplayer with two real clients has not been run; a second player is simulated through the server API (unit tests, and team create, invite, join, shared completion and disband in a real client).
