---
title: Book settings
summary: Switch editing off, give the Quest Book on first join, and the other book.json options.
---
The book's switches live in `config/barelyquesting/book.json`, next to the title and the chapter order. Edit the file by hand (or in the starter book the mod writes) and run `/barelyquesting reload`; nothing needs a restart.

| Field | Values | Default | What it does |
|---|---|---|---|
| `editing` | `"on"`, `"off"` | `"on"` | `"off"` hides the **Edit** button and refuses every edit, `/barelyquesting edit` and the editor packets, for everyone including operators. Use it for a finished questline. Scripts, `/barelyquesting import` and `/barelyquesting reload` keep working, so the pack author still changes the book by editing the files and reloading. |
| `creative_edits` | `true`, `false` | `true` | Whether players in creative mode may edit (when `editing` is on). |
| `editors` | list of names | empty | Players allowed to edit besides operators. |
| `give_book_on_join` | `true`, `false` | `false` | Gives each player one **Quest Book** the first time they join the world; it is dropped at their feet if the inventory is full. A rejoin or a death never repeats it. |
| `open_book_on_join` | `true`, `false` | `false` | Opens the book on a player's first join to the world. |

The first-join list is kept per world in `barelyquesting/welcomed.json` inside the world folder. Delete a player's entry to welcome them again.
