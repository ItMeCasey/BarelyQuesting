---
title: Config files
summary: The .toml settings, the in-game config screen and migrating from older versions.
---
The mod's settings are standard Forge and NeoForge config files, written to `config/` the first time the game starts, with a comment above every value.

| File | Side | Keys |
|---|---|---|
| `barelyquesting-common.toml` (`barelyquesting-startup.toml` on NeoForge 26.3) | server, and singleplayer worlds | `editing`, `creative_edits`, `editors`, `give_book_on_join`, `open_book_on_join` |
| `barelyquesting-client.toml` | each player's own game | `hud_tracker`, `theme`, `view`, `last_chapter`, `pinned_quests` |

On NeoForge the values also appear in the in-game config screen (Mods, Barely Questing, Config). Forge has no config screen; edit the file.

| Key | Values | Default | What it does |
|---|---|---|---|
| `editing` | `true`, `false` | `true` | `false` hides the **Edit** button and refuses every edit, `/barelyquesting edit` and the editor packets, for everyone including operators. Use it for a finished questline. Scripts, `/barelyquesting import` and `/barelyquesting reload` keep working, so the pack author still changes the book by editing the quest files and reloading. |
| `creative_edits` | `true`, `false` | `true` | Whether players in creative mode may edit (when `editing` is on). |
| `editors` | list of names | empty | Players allowed to edit besides operators. `/barelyquesting editor add` and `remove` change this list. |
| `give_book_on_join` | `true`, `false` | `false` | Gives each player one **Quest Book** the first time they join the world; it is dropped at their feet if the inventory is full. A rejoin or a death never repeats it. |
| `open_book_on_join` | `true`, `false` | `false` | Opens the book on a player's first join to the world. |
| `hud_tracker` | `true`, `false` | `true` | Shows the pinned-quest tracker on the game screen. |
| `theme` | theme id | empty | The book theme, for example `barelyquesting:parchment`. Empty is the default theme. |
| `view` | `graph`, `list` | `graph` | How a chapter is shown. |
| `last_chapter` | chapter id | empty | The chapter the book reopens on. |
| `pinned_quests` | list of quest ids | empty | Quests pinned to the tracker. |

The quest book itself (title, chapter order and every quest) is data, not settings: it stays in `config/barelyquesting/book.json` and `chapters/*.json`. The first-join list is kept per world in `barelyquesting/welcomed.json` inside the world folder; delete a player's entry to welcome them again.

## Migrating

Before version 2026.11.9 the settings lived in `config/barelyquesting/book.json` (the first five keys above) and `config/barelyquesting/client.json` (the rest). The first time 2026.11.9 or later starts and a new toml file does not exist yet, every value you had set is copied into it. Then the old file is kept as a backup next to itself: `client.json` becomes `client.json.back`, and `book.json` (which still holds the book's title and chapters) is saved as `book.json.back` and rewritten without the setting keys. The first line of a `.back` file says where and why it migrated; the rest is the original file. A `.back` file is never overwritten (`.back2`, `.back3` are used instead). If an old file is corrupt or only partly readable it is left exactly as it is and the unreadable values use their defaults. The migration runs only when the toml does not exist, so it never changes a toml you edited. To go back to an older version, put the `.back` file's content (without its first line) back in place of the file.
