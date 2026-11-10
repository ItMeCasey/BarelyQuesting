# Scripting

ModJS and KubeJS are optional. The functional reference layer is NeoForge on Minecraft 26.1.2. Other loader/version adapters are unfinished.

## ModJS

Use `BarelyQuesting` in `modjs/startup_scripts/` to register callbacks and define content. The bridge uses ModJS's `ExtraBindings.register(BindingProvider)` extension API and does nothing when ModJS is absent. ModJS 2026.11.30 (a local development snapshot containing payload fix 3768dfd) passed dedicated-server creation/callback checks and the real-client startup/fresh-callback/progress-sync check. Its SHA-256 is recorded in docs/integrations.md. The older 2026.11.23 jar failed client loading with a missing `modjs:data` client handler; that blocker is resolved in the tested snapshot.

## KubeJS

On first startup with KubeJS installed, Barely Questing creates `kubejs/startup_scripts/barelyquesting.js`. It assigns the shared API to `global.BarelyQuesting` through KubeJS's `Java.loadClass`. Existing files are never overwritten. An I/O failure is logged and installation reports failure. Deleting the helper causes it to be recreated on the next launch with KubeJS installed.

Use `global.BarelyQuesting`, not an unqualified `BarelyQuesting`, in KubeJS scripts. Task, reward and lock types register by name, so registering again replaces them. Event listeners add up: start a server script that registers listeners with `global.BarelyQuesting.clearListeners()` and a `/reload` replaces them (two real reloads on KubeJS 8.0.8 left one listener firing; fixture `scripts/integration/kubejs-reload.js`). Name content scripts after the helper, for example `zz_quests.js`, so the global exists when they run. Existing helpers from earlier builds can continue to work, but replace their content manually to use the current shared API if desired:

```js
global.BarelyQuesting = Java.loadClass('com.itmecasey.barelyquesting.core.script.Bridges').API
```

KubeJS 26.1.2-8.0.8 passed a real-client check: startup creation, functional-interface callback execution, completion and client synchronization. This is a helper-script bridge, not a native KubeJS event plugin.

## Example

For ModJS, set `var quests = BarelyQuesting`. For KubeJS:

```js
var quests = global.BarelyQuesting
quests.chapter('pack', 'Pack Quests', 'minecraft:book')
quests.registerTask('high_place', ctx => ctx.y() > 100 ? ctx.target() : ctx.current())
quests.registerReward('celebrate', ctx => { ctx.giveItem('2 minecraft:diamond'); ctx.addXp(10) })
quests.quest('pack', JSON.stringify({
  id: 'climb', title: 'Climb above Y 100',
  tasks: [{id: 'height', type: 'scripted', name: 'high_place', count: 1}],
  rewards: [{id: 'gift', type: 'scripted', name: 'celebrate'}]
}))
quests.onQuestCompleted(event => console.info('Completed ' + event.questId()))
```

Creation before a server starts is queued until attachment. IDs are stable: defining the same chapter updates its header, and defining the same quest replaces its definition. Content edits save and sync through the server's edit pipeline. Progress queries and mutations require a running server; call `isReady()` to check. Run server mutations on the server thread.

## API reference

| Purpose | Methods |
| --- | --- |
| Content | `chapter(id, title, icon)`, `quest(chapterId, jsonText)`, `removeQuest(id)`, `removeChapter(id)`, `bookJson()` |
| Scripted task | `registerTask(name, ctx => progress)`, `registerTaskEvents(name, ctx => amountToAdd)` |
| Scripted reward | `registerReward(name, ctx => ...)` |
| Script lock | `registerLock(name, ctx => locked)`; true keeps the quest closed |
| Custom types | `newTaskType(type, defaultTarget, ctx => progress)`, `newRewardType(type, ctx => ...)` |
| Events | `onQuestStarted`, `onTaskProgressed`, `onQuestCompleted`, `onQuestClaimed`; `clearListeners()` drops all of them (call it first in a server script so a reload replaces the listeners instead of doubling them) |
| Progress | `isComplete(playerUuid, questId)`, `taskProgress(playerUuid, questId, taskId)`, `complete(playerUuid, questId)`, `addProgress(playerUuid, questId, taskId, amount)`, `reset(playerUuid, questId)` |
| Teams | `teamName(playerUuid)` |

Task callbacks return absolute progress; event callbacks return an amount to add. Callback contexts expose player IDs/names, position, dimension, biome, inventory counts, statistics and definition parameters. Reward contexts expose item, XP, level, command and message helpers. Events expose `questId()`, `questTitle()`, `teamId()`, `taskId()` and `playerId()`; player ID may be empty when the event has no player.

Runnable startup fixtures used by the integration checks are in `scripts/integration/`. The shared Rhino unit tests also exercise custom task/reward types, locks, rewards, event tasks and listeners. These tests do not substitute for validation of every callback in KubeJS's fork of Rhino.
