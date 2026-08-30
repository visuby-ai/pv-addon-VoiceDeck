Plasmo Surveillance - Tape Player UI v10

Based on the v9 project supplied by the user.

UI changes:
- Replaced the old plain frame with a new custom retro cassette / surveillance chassis.
- No vanilla Button widgets.
- No font glyph arrows: < > are drawn as pixel icons.
- Minus and plus are drawn as pixel icons.
- Import and Play use custom pixel icons and centered labels.
- Dynamic labels use drawCenteredString for stable centering at GUI Scale 3.
- Hitboxes match the visible controls.
- UI logical size is 360x260 and is centered on screen.
- Existing tape selection, range, WAV import, drag-and-drop and playback logic is preserved.

Build on the user's machine with:
  ./gradlew build

The build was not executed in this environment because the Gradle wrapper
requires downloading Gradle 8.8 and outbound network access is unavailable.

UI v11: fully custom console; no tape_player_frame texture or vanilla button widgets.
