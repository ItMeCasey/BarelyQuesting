---
title: Quests, tasks, rewards and locks
summary: How a quest is built and what each task, reward and lock type does.
---
A **quest** has a title, a description, an icon, a position on the chapter's grid, dependencies, tasks, rewards and locks. A quest is complete when all its tasks are done (or any one, with **Any task**), and it opens when its dependencies are complete and no lock is closed. **Optional** quests never block others; **Hidden** quests stay out of sight until their dependencies are done.

## Tasks

| Type | What counts |
|---|---|
| `item` | Hold the items (counted from the inventory), or with `consume` hand them in with the Submit button. Matches ids, tags (`#minecraft:logs`) and, if asked, item data. |
| `craft` | Crafting or smelting the item. |
| `obtain` | Picking up or crafting the item, even if you give it away later. |
| `kill` | Kill an entity type or tag, `count` times. |
| `advancement` | Earn an advancement. |
| `statistic` | A game statistic (`custom`, `mined`, `crafted`, `used`, `broken`, `picked_up`, `dropped`, `killed`, `killed_by`) reaches a value. |
| `location` | Be within `radius` blocks of x y z, optionally in one dimension. |
| `dimension`, `biome`, `structure` | Be in a dimension, a biome (or biome tag), or inside a structure. |
| `checkmark` | The player ticks it off. |
| `scripted` | Decided by a script, see [[scripting]]. |

Progress never goes down, so a quest stays done.

## Rewards

| Type | Gives |
|---|---|
| `item` | Items (what does not fit drops at the player's feet). |
| `xp` | Experience `points` or `levels`. |
| `command` | Runs a command as the server with the player as `@s`; `{player}` is the name. |
| `scripted` | Whatever the script does. |

Each reward has a **scope**: `player` (every team member can claim it) or `team` (claimed once for the whole team). `auto: true` claims it the moment the quest completes.

## Locks

`quest` (another quest must be done), `time` (open from and/or until a date) and `script` (a script condition).

## Files

```json
{
  "id": "wood",
  "title": "Gather Wood",
  "description": "Punch a tree and **collect 4 logs**.",
  "icon": "minecraft:oak_log",
  "x": 0, "y": 0,
  "dependencies": [],
  "tasks": [{ "id": "t0", "type": "obtain", "item": { "tag": "minecraft:logs", "count": 4 } }],
  "rewards": [{ "id": "r0", "type": "item", "item": { "item": "minecraft:apple", "count": 8 } }]
}
```

Descriptions use a small markup: `**bold**`, `*italic*`, `__underline__`, `~~strike~~`, `&c` color codes (`&r` resets) and `[label](https://link)`.
