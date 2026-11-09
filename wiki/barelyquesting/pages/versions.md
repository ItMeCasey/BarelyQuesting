---
title: Versions and known gaps
summary: What was checked on each build, and what was not.
---
All ten builds compile, run the shared unit tests and pass a headless dedicated-server self test (a real player, item and tag matching, the engine, saving, commands). The screen, themes, editor, toasts, HUD, viewers and scripting bridges were run on a real client on **NeoForge 26.1.2** only. The other nine builds are **not yet seen in a real client**.

| Build | Server self test | Real client |
|---|---|---|
| NeoForge 26.1.2 | pass | yes |
| Forge 26.1.2, NeoForge and Forge 26.2, 26.3 | pass | not yet |
| NeoForge and Forge 1.21.1 | pass | not yet |
| NeoForge and Forge 1.20.1 | pass | not yet |

## Known gaps

- No craftable Quest Book item yet; use J or `/barelyquesting open`.
- No viewer button that opens the quest for an item.
- The editor's item picker lists registry items; it is not driven by JEI or BEI search.
- KubeJS support is a helper script, not a native plugin, and KubeJS has no build for 26.2, 26.3, or Forge 1.21.1 and 26.x. EMI has no build for 26.1.2 to test with.
- BEI lookups are wired on the 26.x builds only.
- Multiplayer with two real clients has not been run; the server and wire format are unit tested.
