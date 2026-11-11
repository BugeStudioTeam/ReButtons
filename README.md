# ReButtons

ReButtons is a small Android utility that provides on-screen replacements for the physical volume and power buttons. It is intended for devices whose hardware buttons are broken, worn out, or difficult to press.

The app runs as an Accessibility Service and shows a floating panel on the screen edge. The panel can be dragged, snapped to the nearest edge, and collapses into a small circular button after a configurable idle delay.

## Features

- Volume up and volume down controls with press-and-hold repeat
- Power menu button using the system global action
- Draggable floating panel with edge snapping
- Auto-collapse into a compact circular button
- Adjustable component scale
- Adjustable collapse delay
- Toggle for the volume group
- Toggle for the power group
- Material Design 3 Expressive styling with dynamic color on Android 12 and above

## Requirements

- Android 8.0 (API 26) or higher
- Accessibility Service permission granted by the user

## Usage

1. Install and open the app once to review the settings.
2. Open system settings, go to Accessibility, and enable the ReButtons service.
3. The floating panel appears on the screen edge.
4. Tap the volume buttons to change volume, or press and hold them to repeat.
5. Tap the power button to open the system power menu.
6. Drag the panel to move it. It snaps to the nearest screen edge when released.
7. After the configured idle delay the panel collapses into a small circular button. Tap the circle to expand it again.

## Settings

- Component size: scales the entire panel between 75% and 150%.
- Collapse delay: idle time before the panel collapses, from 1 to 10 seconds.
- Volume controls: show or hide the volume buttons.
- Power menu: show or hide the power menu button.

At least one control group must remain enabled.

## Permissions

ReButtons only uses the Accessibility Service permission. It does not request network, storage, or other sensitive permissions. The service does not read window content and only performs global actions and volume adjustments.

## License

This project is licensed under the GNU General Public License v3.0. 
