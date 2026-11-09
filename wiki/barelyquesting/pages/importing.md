---
title: Importing FTB Quests and Better Questing
summary: Convert an existing book once.
---
Run `/barelyquesting import <path>` with the path to:

- an **FTB Quests** `quests` folder (the one with `chapters/*.snbt`; its `lang/en_us.snbt` is used for titles and descriptions) or a single chapter `.snbt` file, or
- a **Better Questing** `DefaultQuests` folder (`QuestDatabase.json`, `QuestLineDatabase.json`).

Relative paths start in the server folder. The result is added as new chapters in one step, so a single **Undo** takes it back, and it is saved as normal chapter files. Quest ids are made new, dependencies between quests (also across chapters) are kept, and quest positions are mapped to the grid.

What carries over: item, kill, advancement, location, dimension, biome, structure, statistic and checkmark tasks; item, XP and command rewards; titles, descriptions, icons. Anything without an equivalent (loot or choice rewards, observation tasks, custom tasks) is skipped or becomes a checkmark task, and the count is shown in the log. There is no runtime link to either mod.
