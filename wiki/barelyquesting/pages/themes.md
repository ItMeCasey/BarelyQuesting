---
title: Themes
summary: Change the look of the book with a resource pack.
---
Open **Themes** in the book to choose one. **Default** is a dark panel style drawn with plain fills; **Parchment** is warm paper drawn with sprites. Add more with a resource pack and press F3+T.

## Making a theme

`assets/<namespace>/barelyquesting/themes/index.json` lists your theme ids and each id has a file next to it:

```json
{ "themes": ["mytheme"] }
```

```json
{
  "name": "My Theme", "author": "You", "font": "", "node_size": 26,
  "colors": { "panel": "#2b2b33", "text": "#ededf5", "node_complete": "#4fc36b" },
  "sprites": {
    "panel": { "texture": "mymod:textures/gui/panel.png", "size": [32, 32], "border": [6, 6, 6, 6] }
  },
  "sounds": { "click": "minecraft:ui.button.click", "complete": "minecraft:entity.player.levelup" }
}
```

Anything you leave out falls back to the built-in values, so a theme can be a few colors. Colors are `#RRGGBB` or `#AARRGGBB`. Sprites are a texture, its size, an optional `region` and nine-slice `border` values (left, top, right, bottom).

Color names: `background`, `panel`, `panel_edge`, `panel_inset`, `header`, `text`, `text_muted`, `title`, `accent`, `button`, `button_hover`, `button_off`, `tab`, `tab_selected`, `slot`, `bar`, `bar_fill`, `line`, `line_done`, `node_locked`, `node_available`, `node_progress`, `node_complete`, `node_claimed`, `node_selected`, `danger`, `success`.

Sprite names: `panel`, `panel_inset`, `header`, `button`, `button_hover`, `button_off`, `tab`, `tab_selected`, `slot`, `bar`, `node_locked`, `node_available`, `node_progress`, `node_complete`, `node_claimed`.

Sound keys: `click`, `complete`, `claim`, `open`.
