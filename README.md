# TalismanBag

A Minecraft plugin for Paper 1.21.4 that gives players a dedicated bag to store and passively activate talisman items from MMOItems.

## Features

- Dedicated GUI bag (configurable size, up to 45 slots) to store MMOItems talismans
- Talismans stored in the bag remain **passively active** — their stats and effects apply as if they were equipped
- Full MMOItems integration via the `PlayerInventory` API — stat recalculation is automatic
- Duplicate talisman protection (configurable limit per talisman ID)
- Whitelist mode: accept any item with the `TALISMAN` MMOItems type, or restrict to a specific ID list
- Fully configurable messages with `&` color codes and hex `&#RRGGBB` support
- Graceful fallback if MMOItems is not installed

## Requirements

| Dependency | Version | Required |
|------------|---------|----------|
| Paper | 1.21.4+ | Yes |
| MMOItems | 6.9.5+ | Soft (recommended) |
| MythicLib | 1.6.2+ | Soft (required by MMOItems) |

## Installation

1. Download `TalismanBag-x.x.x.jar` from the [releases page](../../releases)
2. Drop it into your server's `plugins/` folder
3. Restart the server
4. Edit `plugins/TalismanBag/config.yml` to your liking
5. Run `/talismanbag reload` to apply changes without restarting

## Commands

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/talismanbag` | `/tbag`, `/talisman` | Open your talisman bag | `talismanbag.open` |
| `/talismanbag open` | | Open your talisman bag | `talismanbag.open` |
| `/talismanbag reload` | | Reload the configuration | `talismanbag.reload` |
| `/talismanbag info` | | Show plugin info and active talismans | `talismanbag.info` |

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `talismanbag.open` | Everyone | Open the talisman bag |
| `talismanbag.info` | Everyone | View plugin info |
| `talismanbag.reload` | OP | Reload the configuration |
| `talismanbag.bypass.limit` | OP | Bypass the talisman slot limit |

## Configuration

```yaml
# Number of talisman slots (must be a multiple of 9, max 45)
bag-size: 27

# MMOItems item type recognized as a talisman
talisman-item-type: "TALISMAN"

# true  → accept any item with the above MMOItems type
# false → only accept items listed in allowed-talismans
use-type-whitelist: true

# Specific MMOItems IDs allowed (only used if use-type-whitelist is false)
allowed-talismans:
  - "FIRE_TALISMAN"
  - "WATER_TALISMAN"
  - "EARTH_TALISMAN"

# Maximum number of the same talisman allowed at once (1 = no duplicates)
max-duplicate-talismans: 1

# Messages — supports & color codes and hex &#RRGGBB
messages:
  prefix: "&#FFD700[TalismanBag] &r"
  bag-opened: "&aYour Talisman Bag has been opened!"
  talisman-activated: "&aThe talisman &e{name} &ahas been activated!"
  talisman-deactivated: "&cThe talisman &e{name} &chas been deactivated."
  not-a-talisman: "&cThat item is not a recognized Talisman!"
  duplicate-blocked: "&cYou already have that talisman active!"
  bag-full: "&cYour Talisman Bag is full!"
  no-permission: "&cYou don't have permission to do that."
  reloaded: "&aConfiguration reloaded successfully!"

# GUI appearance
gui:
  title: "&#FFD700✦ &6Talisman Bag &r&#FFD700✦"
  filler-enabled: true
  filler-material: "GRAY_STAINED_GLASS_PANE"
  filler-name: " "
```

## How It Works

TalismanBag registers itself as a custom `PlayerInventory` source with MMOItems. Whenever MMOItems recalculates a player's stats, it queries the bag for its contents and treats those items as equipped — so all talisman stats, abilities, and set bonuses apply automatically without the player needing to hold or wear the items.

## Building from Source

Requires Java 21 and Maven.

```bash
git clone https://github.com/Klonithorium/MMOTalismanBag.git
cd MMOTalismanBag
mvn clean package
# Output: target/TalismanBag-x.x.x.jar
```

## Author

**klonithorium**
