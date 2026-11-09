---
title: Troubleshooting
summary: When something does not work.
---
**J does nothing.** Check Controls, Barely Questing. The mod must be on the server too; the book stays empty if the server does not have it.

**The book is empty or old.** Run `/barelyquesting reload` after editing the JSON files by hand. Files that are not valid JSON are skipped and named in the server log.

**A task does not count.** Only open quests count. Check that every dependency is complete and no lock is closed (the panel names what is missing). Item tasks match by id; use a tag (`#minecraft:logs`) for variants, and turn item data matching on only when you need it.

**I cannot see Edit.** You need to be an operator, in creative mode, or on the editor list (`/barelyquesting editor add <name>`).

**I lost a quest.** Use Undo in the editor right away, or restore `config/barelyquesting/chapters/<chapter>.json` from a backup; the files are plain JSON.

**A theme is missing.** The pack needs `assets/<namespace>/barelyquesting/themes/index.json` listing the id; press F3+T.

**Bugs and crashes:** [github.com/ItMeCasey/BarelyQuesting/issues](https://github.com/ItMeCasey/BarelyQuesting/issues) (include the mod version, loader and Minecraft version).
