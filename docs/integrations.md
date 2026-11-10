# Optional integrations: NeoForge 26.1.2

Validated on 2026-11-08 with NeoForge 26.1.2.125 and Java 25. Other loader/version ports remain unfinished.

| Integration | Version tested | Result |
| --- | --- | --- |
| ModJS | 2026.11.31 (sha256 c9944fde5c545cca323ee5edcce064a856b242b3ccc13c09052404cfe2818af0; the 2026.11.30 snapshot passed the same check earlier) | Real client: startup definition, reset-and-poll callback completion and client progress sync passed; screenshot inspected. Dedicated server: all 35 checks passed. |
| KubeJS | 26.1.2-8.0.8 | Real client: helper loading, startup quest creation, callback completion and client sync passed. |
| BEI | 2026.11.7 | Real client: recipes, usages, popup cleanup and return to the quest book all passed; screenshots inspected. |
| JEI | 29.53.0.115 | Real client: oak plank recipes and usages opened correctly; both screenshots inspected. |
| REI | 26.1.819 | Real client: oak plank recipes and usages opened correctly; both screenshots inspected. |
| EMI | none listed for this target | No compatible NeoForge 26.1.2 version was returned by the publisher's Modrinth project version API on the validation date. Bridge retained but not client-tested. |
| No optional mods | reference build | Client opened the quest book; viewer lookup returned false without changing screens. Dedicated server passed all 33 base checks. |

Recipe items use left click for recipes and right click for usages. Viewer priority is BEI, EMI, REI, JEI. Each attempt handles its own reflection, runtime and linkage errors so a broken optional viewer cannot prevent a later viewer from handling the click. Exact method parameter types avoid overload-order dependence. JEI reflection invokes public API interfaces to support non-public implementation classes.

BEI currently has no public lookup API. The adapter reflects its existing `RecipePopup.open(Item, boolean)` method and supplies a container screen host. Lookup matches the item type, not item components. The host restores the quest screen and closes the popup when dismissed. JEI runtime acquisition still uses `mezz.jei.common.Internal`; a future JEI internal change can require an adapter update. All these integrations remain optional and are absent from the release jar.

## Reproduce

`neoforge-26.1.2/build.gradle` offers a development-only `integrationMods` property. Put published mod jars and required dependencies in `.integration-cache/<profile>/`. Profiles used here: `modjs`, `kubejs`, `bei`, `jei`, `rei`. This cache is ignored by Git, and runtime dependencies are not bundled. The property requires `-PdevTools`.

KubeJS dependencies used: Better Advanced Tooltips 2601.1.0-build.11 and Rhino 2101.2.8-build.91. JEI dependency: MezzConfig 0.6.11 standalone. REI dependencies: Cloth Config 26.1.154 and Architectury 20.1.17. Upstream downloads were selected through `https://api.modrinth.com/v2/project/<slug>/version` filtered to `game_versions=["26.1.2"]` and `loaders=["neoforge"]`, and their SHA-512 hashes were verified before use. BEI and ModJS jars came from the local projects.

Copy `scripts/integration/modjs-startup.js` into `run/modjs/startup_scripts/zz_bq_validation.js`, or the KubeJS fixture into `run/kubejs/startup_scripts/zz_bq_validation.js`. The generated KubeJS helper must load before that fixture. Run from `neoforge-26.1.2` in PowerShell:

```powershell
$env:BARELYQUESTING_TOUR = '1'
$env:BARELYQUESTING_INTEGRATIONS = 'kubejs'
$env:BARELYQUESTING_VIEWER = 'jei'
$env:BARELYQUESTING_INTEGRATION_ONLY = '1'
.\gradlew.bat runClient -PdevTools '-PintegrationMods=kubejs,jei' --no-daemon
```

For viewer-only runs omit `BARELYQUESTING_INTEGRATIONS` and select the viewer profile. Use viewer `none` and omit `integrationMods` to check the no-viewer path. The tour resets the integration quest, polls its script callback again, flushes updates, and asserts client progress and completion, so saved progress cannot supply a false pass. It uses game handlers, captures screenshots, logs assertions, and exits. **Check for `BQ-TOUR FAILED` as well as `BQ-TOUR DONE`; Gradle success alone is not a tour pass.** Screenshots are in `run/screenshots/`; reviewed copies are in `docs/frames/`.

For dedicated-server ModJS checks set `BARELYQUESTING_SELFTEST=1` and `BARELYQUESTING_INTEGRATIONS=modjs`, then run `runServer -PdevTools -PintegrationMods=modjs`. Without the integration variable and property the self-test runs the 33 base checks. Read `SELFTEST DONE failures=0`; the server stops itself.

Use the development run directory only for disposable validation data. Do not run parallel Minecraft clients. Run the local `who-owns-minecraft.ps1` check before client launches and put the game on DISPLAY6 with `place-window.ps1`.

## Remaining gaps

ModJS 2026.11.23 had a missing `modjs:data` client handler. The tested local 2026.11.30 development snapshot fixes that blocker (payload fix 3768dfd, plus pending HUD/JEI changes); it is not a published release. SHA-256: `882a0aeffeb5ed156ef45cbb79c9f1c8a968a8930e36a8a2d394e6862f4b7dc6`. Its logs also report a ModJS-generated global-loot-modifier JSON parse error, forwarded to the ModJS agent; it did not prevent these quest integration checks. KubeJS support uses a startup helper instead of a native plugin; registration cleanup during server-script reloads is not implemented, so restart after changing callback registrations. EMI is unverified on this target. The general client tour previously logged a failed `Claim all` button action against reused test data; broad editor/reward/keybind acceptance has not been claimed by these focused integration checks.
