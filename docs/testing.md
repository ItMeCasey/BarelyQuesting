# Testing

- 2026-11-14 (version 2026.11.9): 87 unit tests pass (NeoForge 1.21.1), 17 of them for the config migration with real 2026.11.1 and 2026.11.7 files. Real clients on NeoForge 1.21.1 and Forge 1.21.1 migrated the old JSON settings to the toml files (old files kept as .back) and ran the full tour. All nine loader config classes compile.
- 2026-11-13 (version 2026.11.8): 71 unit tests pass (NeoForge 1.21.1). Client tour on NeoForge 1.21.1 and Forge 1.21.1: all checks pass, including the dialog close, fit and Esc check. Frames of the Themes dialog, the Team dialog and a selected quest with the hint text removed: `docs/frames/neoforge-1.21.1-nohint-*.png` and `docs/frames/forge-1.21.1-nohint-*.png`.
- 2026-11-12 (version 2026.11.7): dialog, editing-off and first-join checks on NeoForge 1.21.1; key and dialog checks on NeoForge 26.3.
- Recipe viewers: see `docs/integrations.md` and `docs/COMPATIBILITY.md`.
