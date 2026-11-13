---
title: Scripting with ModJS and KubeJS
summary: Create quests, add types and listen to events from scripts.
---
With ModJS or KubeJS installed, scripts get a `BarelyQuesting` object. Neither mod is required.

## ModJS

The global `BarelyQuesting` is available in startup and server scripts; client scripts do not get it, since quests run on the server. Quests made in startup scripts are queued until the world loads.

## KubeJS

Barely Questing writes `kubejs/startup_scripts/barelyquesting.js` once, which sets `global.BarelyQuesting`. Use `global.BarelyQuesting` in your scripts. Task, reward and lock types are replaced by name when a script registers them again. Event listeners add up, so start a server script that registers listeners with `global.BarelyQuesting.clearListeners()`: after a reload it then replaces the old listeners instead of doubling them.

## What you can do

```js
const BQ = global.BarelyQuesting   // KubeJS; in ModJS use BarelyQuesting directly

BQ.chapter('my_pack', 'My Pack', 'minecraft:diamond')
BQ.quest('my_pack', JSON.stringify({
  id: 'sky', title: 'Touch the sky',
  tasks: [{ id: 't', type: 'scripted', name: 'high', count: 1 }],
  rewards: [{ id: 'r', type: 'scripted', name: 'gift' }]
}))

// a scripted task: return the progress to set (higher than now to advance)
BQ.registerTask('high', ctx => ctx.y() > 200 ? ctx.target() : ctx.current())
// a scripted reward
BQ.registerReward('gift', ctx => { ctx.giveItem('2 minecraft:diamond'); ctx.addLevels(1) })
// a lock: return true to keep the quest closed
BQ.registerLock('night_only', ctx => false)
// events
BQ.onQuestCompleted(e => console.log(e.questId() + ' done for team ' + e.teamId()))
```

| Call | Does |
|---|---|
| `chapter(id, title, icon)` | Adds or updates a chapter. |
| `quest(chapterId, json)` | Adds or replaces a quest from JSON in the file format. Give it an `id` so it is replaced, not duplicated, each start. Returns the id. |
| `removeQuest(id)`, `removeChapter(id)` | Removes them. |
| `registerTask(name, fn)` | Polled scripted task (`{"type":"scripted","name":name}`). |
| `registerTaskEvents(name, fn)` | Event-driven scripted task; `fn` gets `kind()` (`kill`, `craft`, `pickup`, `advancement`), `key()`, `amount()` and returns the progress to add. |
| `newTaskType(type, defaultTarget, fn)`, `newRewardType(type, fn)` | A whole new type usable as `{"type": ...}`. |
| `registerReward(name, fn)`, `registerLock(name, fn)` | Scripted reward and lock. |
| `onQuestStarted`, `onTaskProgressed`, `onQuestCompleted`, `onQuestClaimed` | Events with `questId()`, `questTitle()`, `teamId()`, `taskId()`, `playerId()`. |
| `complete(playerUuid, questId)`, `reset(playerUuid, questId)`, `addProgress(playerUuid, questId, taskId, n)` | Change progress for the player's team. |
| `isComplete(playerUuid, questId)`, `taskProgress(...)`, `teamName(playerUuid)`, `bookJson()` | Read state. |

Contexts: task functions get `player` helpers (`playerId()`, `playerName()`, `x() y() z()`, `dimension()`, `biome()`, `count(itemSpec)`, `hasAdvancement(id)`, `stat(type, key)`), parameters (`param(key, default)`, `number(key, default)`) and `current()` and `target()`. Reward functions get `giveItem`, `addXp`, `addLevels`, `command` and `message`.

Changes made by scripts go through the same path as the editor, so they are saved and synced.
