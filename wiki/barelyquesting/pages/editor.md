---
title: The in-game editor
summary: Build and change the book without leaving the game.
---
The **Edit** button appears for operators, players in creative mode (unless turned off) and names on the editor list (`/barelyquesting editor add <name>`). `/barelyquesting edit` opens the book in edit mode. A pack that ships a finished questline can turn editing off for everyone; see [[settings]].

- **Move** a quest by dragging its node; it snaps to the grid.
- **Add** a quest with **+ Quest** or by right-clicking an empty spot. **+ Chapter** adds a chapter; click the selected chapter again to rename it, set its icon and group, reorder it or delete it.
- **Link**: select a quest, press **Link**, click the quest that should depend on it. Click again to remove the link. Loops are refused.
- **Fields**: select a quest to edit its title, description, icon, grid position, Optional, Hidden and Any task flags, dependencies, tasks, rewards and locks. Each task, reward and lock has its type and its own fields; items open a searchable picker (items or tags, with a count).
- **Undo and redo** with the buttons or Ctrl+Z and Ctrl+Y. Deleting a quest or chapter can be undone.
- The eye button in the panel previews a quest the way a player sees it.

Every change goes to the server, which saves the files and shows it to everyone online at once. Text fields are sent when you press Enter or click away.
