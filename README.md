# Heal Splats

Shows a hitsplat on your character whenever you eat food or drink a healing potion, displaying the exact amount of HP you recovered.

## Features

- Splat appears mid-body and tracks your character as you move
- Displays the actual HP gained, not the food's max heal value (e.g. eating a shark at full HP - 3 shows **+3**, not +20)
- Works with all food and any potion with a Drink option that restores HP (Saradomin brew, Guthix rest, etc.)
- Fully customisable via the plugin config panel

## Configuration

| Option | Default | Description |
|---|---|---|
| Duration (seconds) | 2 | How long the splat stays on screen (1–10 s) |
| Outer colour | `#1A5C1A` | Hex code for the jagged outer ring |
| Centre colour | `#228B22` | Hex code for the bright inner circle |
| Shine colour | `#4CAF50` | Hex code for the small highlight fleck |

## Limitations

The splat only triggers on deliberate **Eat** or **Drink** clicks, so the following heal sources will not show a splat:

- Phoenix necklace (automatic trigger)
- Locator orb (`Feel` option)
- Passive HP regeneration
- Guthan's set effect
