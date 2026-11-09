---
title: Versions and known gaps
summary: What was checked on each build, and what was not.
---
All ten builds compile, run the shared unit tests and pass a headless dedicated-server self test (a real player, item and tag matching, the engine, saving, commands). Every build was also run once on a real client (open the book, graph and list, themes, editor, team, claim a reward, HUD and toasts, find a quest by item). Viewers and the ModJS and KubeJS bridges were run on **NeoForge 26.1.2** only. The Forge and NeoForge 1.20.1 jars were also loaded in real Forge and NeoForge installs.

| Build | Server self test | Real client |
|---|---|---|
| NeoForge 26.1.2 | pass | yes, plus viewers and scripting |
| Forge 26.1.2, NeoForge and Forge 26.2, 26.3 | pass | yes |
| NeoForge and Forge 1.21.1 | pass | yes |
| NeoForge and Forge 1.20.1 | pass | yes, and a real Forge and NeoForge install |

## Known gaps

- The Quest Book item (a book and a gold nugget) is new in 2026.11.2 and has not been tried in a real client. J and `/barelyquesting open` always work.
- Ctrl+J over an item opens its quest. The lookup was checked; picking the hovered item out of JEI, REI, EMI and BEI was not tried in a live viewer.
- The editor's item picker lists registry items; it is not driven by JEI or BEI search.
- KubeJS support is a helper script, not a native plugin, and KubeJS has no build for 26.2, 26.3, or Forge 1.21.1 and 26.x. EMI has no build for 26.1.2 to test with.
- BEI lookups are wired on the 26.x builds only.
- Multiplayer with two real clients has not been run; the server and wire format are unit tested.
