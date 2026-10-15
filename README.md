# Better Right Click

A RuneLite plugin that lets you fully customise the look of the right-click menu.

The vanilla menu is the same brown box for everyone, and the few existing alternatives only give you one other style to pick from. This plugin is built around customisability: every colour, font and style choice is yours, and you can save your setups and share them with other players. It only changes how the menu looks. The game still handles every entry and click exactly as before.

![Vanilla menu](docs/vanilla.png)
*The default RuneLite menu.*

![Dark theme](docs/dark.png)
*One of the built-in themes.*

![Glass theme](docs/glass.png)
*A translucent theme with rounded corners.*

![Theme editor](docs/editor.png)
*The sidebar panel used to edit themes and settings.*

## Features

### Themes
- Built-in themes: Dark, Midnight, Light, Glass and Classic.
- Edit any theme live and save your changes as your own theme.
- Rename, duplicate and delete your own themes.
- Export a theme to a string and import one from a string, so you can share themes with other players.

### Editor
- A sidebar panel with a live preview of the menu.
- All settings live in one place.

### Style
- Background, border, header, text, hover and accent colours.
- Opacity, corner radius, border, drop shadow and header toggles.

### Hover
- Choose how the hovered row is shown: Fill, Bar, Outline, Text or None.

### Text
- RuneScape regular, small or bold, or Sans and Mono with an adjustable size.
- Optional text shadow.

### Action highlights
- Colour specific options wherever they appear, for example Attack in red.
- Optional bold.

### Targets
- Keep the game's own colours, or set your own for NPCs, players, items and objects.

### Combat levels
- Keep the game's own colours, set your own for lower, same and higher levels, or hide them.

## Getting started

Install Better Right Click from the Plugin Hub. Open its icon in the RuneLite sidebar to pick a theme and adjust settings.

## Sharing themes

Export copies a theme as a string to your clipboard. Import pastes a string in as a new theme.

For example: open the panel, pick the theme you want, click Export, then send the string to a friend. They open their own panel, click Import, paste the string in, and the theme appears in their theme list.

## Compatibility

Interface Styles' HD menu option draws its own menu over this one. Turn it off and set its Menu Alpha to 255, otherwise both plugins will try to draw a menu at once.

## Development

Clone the repository and open it in IntelliJ with JDK 11. Run the `MenuThemesPluginTest` configuration with `-ea` and `--developer-mode`. The run configuration is included in `.run/`.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
